
package com.rodovia.recorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var txtBr: TextView
    private lateinit var txtKm: TextView
    private lateinit var txtVel: TextView
    private lateinit var txtHora: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var lastSpeed = 0.0 // m/s

    private var timer: Timer? = null
    private var logger: TripLogger? = null

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.values.all { it }
        if (allGranted) {
            startCamera()
            startLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        txtBr = findViewById(R.id.txtBr)
        txtKm = findViewById(R.id.txtKm)
        txtVel = findViewById(R.id.txtVel)
        txtHora = findViewById(R.id.txtHora)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        fused = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        btnStart.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopRecording() }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            startCamera()
            startLocation()
        } else {
            permissionsLauncher.launch(permissions)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, vc)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startLocation() {
        try {
            fused.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            lastLat = loc.latitude
            lastLon = loc.longitude
            lastSpeed = (loc.speed.toDouble()).coerceAtLeast(0.0) // m/s

            val chain = Chainage.chainage(lastLat, lastLon)
            val velKmh = lastSpeed * 3.6
            val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            txtBr.text = chain.brRef
            txtKm.text = "km ${'$'}{chain.km}+${String.format("%03d", chain.metros)}"
            txtVel.text = String.format(Locale.getDefault(), "%.1f km/h", velKmh)
            txtHora.text = hora
        }
    }

    private fun startRecording() {
        val vc = videoCapture ?: return
        if (recording != null) return

        val outDir = getExternalFilesDir(null) ?: filesDir
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val videoFile = File(outDir, "trip_${'$'}stamp.mp4")
        val outOpts = FileOutputOptions.Builder(videoFile).build()

        val rec = vc.output
            .prepareRecording(this, outOpts)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { evt ->
                // handle recording events if needed
            }
        recording = rec

        // Start logger
        logger = TripLogger(File(outDir, "logs")).apply { start(System.currentTimeMillis()) }

        // timer de 1s para registrar SRT/CSV
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val chain = Chainage.chainage(lastLat, lastLon)
                val velKmh = lastSpeed * 3.6
                logger?.appendSample(
                    System.currentTimeMillis(),
                    lastLat,
                    lastLon,
                    chain.brRef,
                    chain.km,
                    chain.metros,
                    velKmh
                )
            }
        }, 1000L, 1000L)
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        timer?.cancel()
        timer = null
        logger?.stop()
        logger = null
    }

    override fun onDestroy() {
        super.onDestroy()
        fused.removeLocationUpdates(locationCallback)
        cameraExecutor.shutdown()
    }
}
