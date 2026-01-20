
package com.rodovia.recorder

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class TripLogger(private val dir: File) {
    private var srtFile: File? = null
    private var csvFile: File? = null
    private var srtIndex = 1
    private var lastStartMs = 0L

    private var srtWriter: java.io.BufferedWriter? = null
    private var csvWriter: java.io.BufferedWriter? = null

    fun start(nowMs: Long) {
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(nowMs)
        srtFile = File(dir, "trip_${'$'}stamp.srt")
        csvFile = File(dir, "trip_${'$'}stamp.csv")
        srtWriter = srtFile!!.bufferedWriter()
        csvWriter = csvFile!!.bufferedWriter()
        csvWriter!!.appendLine("timestamp_ms,lat,lon,br,km,metros,vel_kmh")
        srtIndex = 1
        lastStartMs = nowMs
    }

    fun appendSample(nowMs: Long, lat: Double, lon: Double, brRef: String, km: Int, m: Int, velKmh: Double) {
        val endMs = nowMs + 1000
        srtWriter?.apply {
            appendLine(srtIndex.toString())
            appendLine("${'$'}{formatSrt(lastStartMs)} --> ${'$'}{formatSrt(endMs)}")
            appendLine("${'$'}brRef  km ${'$'}km+${String.format("%03d", m)}  |  ${String.format(Locale.US, "%.1f", velKmh)} km/h")
            appendLine("")
        }
        srtIndex++
        lastStartMs = endMs

        csvWriter?.appendLine("${'$'}nowMs,${'$'}lat,${'$'}lon,${'$'}brRef,${'$'}km,${'$'}m,${String.format(Locale.US, "%.1f", velKmh)}")
    }

    fun stop() {
        try { srtWriter?.close() } catch (_: Exception) {}
        try { csvWriter?.close() } catch (_: Exception) {}
    }

    private fun formatSrt(ms: Long): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val msRem = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, msRem)
    }
}
