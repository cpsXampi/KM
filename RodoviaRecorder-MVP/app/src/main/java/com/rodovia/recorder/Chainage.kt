
package com.rodovia.recorder

import kotlin.math.*

data class ChainageResult(
    val brRef: String,
    val km: Int,
    val metros: Int,
    val totalMeters: Double,
    val segmentIndex: Int
)

data class LatLon(val lat: Double, val lon: Double)

object Chainage {
    // Exemplo simples de uma linha BR-116 (região de Roseira/SP), pontos aproximados
    // Substitua por dados oficiais para produção.
    private val brRef = "BR-116"
    private val line: List<LatLon> = listOf(
        LatLon(-22.8698, -45.3020),
        LatLon(-22.8670, -45.2900),
        LatLon(-22.8620, -45.2750),
        LatLon(-22.8560, -45.2600)
    )
    private val cumDist: DoubleArray

    init {
        cumDist = DoubleArray(line.size)
        var acc = 0.0
        cumDist[0] = 0.0
        for (i in 0 until line.size - 1) {
            acc += haversine(line[i], line[i + 1])
            cumDist[i + 1] = acc
        }
    }

    private fun haversine(a: LatLon, b: LatLon): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val sa = sin(dLat / 2)
        val sb = sin(dLon / 2)
        val c = sa * sa + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sb * sb
        return 2 * R * asin(min(1.0, sqrt(c)))
    }

    private data class Projection(val fraction: Double, val distanceMeters: Double)

    private fun projectToSegment(p: LatLon, a: LatLon, b: LatLon): Projection {
        val rLat = Math.toRadians((a.lat + b.lat) / 2.0)
        val mPerDegLat = 111132.954 - 559.822 * cos(2 * rLat) + 1.175 * cos(4 * rLat)
        val mPerDegLon = (Math.PI / 180) * 6378137.0 * cos(rLat)

        fun toXY(pt: LatLon) = Pair((pt.lon - p.lon) * mPerDegLon, (pt.lat - p.lat) * mPerDegLat)

        val A = toXY(a)
        val B = toXY(b)
        val P = Pair(0.0, 0.0)

        val vx = B.first - A.first
        val vy = B.second - A.second
        val wx = P.first - A.first
        val wy = P.second - A.second
        val c1 = vx * wx + vy * wy
        val c2 = vx * vx + vy * vy
        val t = if (c2 <= 1e-6) 0.0 else (c1 / c2).coerceIn(0.0, 1.0)
        val projX = A.first + t * vx
        val projY = A.second + t * vy
        val dist = hypot(projX - P.first, projY - P.second)
        return Projection(t, dist)
    }

    fun chainage(lat: Double, lon: Double): ChainageResult {
        val p = LatLon(lat, lon)
        var bestDist = Double.MAX_VALUE
        var bestChain = 0.0
        var bestSeg = 0
        for (i in 0 until line.size - 1) {
            val proj = projectToSegment(p, line[i], line[i + 1])
            if (proj.distanceMeters < bestDist) {
                bestDist = proj.distanceMeters
                bestSeg = i
                val segLen = cumDist[i + 1] - cumDist[i]
                bestChain = cumDist[i] + proj.fraction * segLen
            }
        }
        val km = (bestChain / 1000.0).toInt()
        val m = (bestChain - km * 1000).roundToInt()
        return ChainageResult(brRef, km, m, bestChain, bestSeg)
    }
}
