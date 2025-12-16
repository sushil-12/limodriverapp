package com.limo1800driver.app.rideinprogress

import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan
import kotlin.math.PI

/**
 * Projects a location onto a route polyline.
 *
 * We intentionally do a light-weight WebMercator projection and a segment projection in 2D.
 * For the small distances involved in navigation UI updates, this produces stable "snap to route"
 * behavior without heavy dependencies.
 */
class RideRouteSnapper(
    private val maxSnapDistanceMeters: Double = 100.0
) {
    fun snap(location: LatLng, polyline: List<LatLng>): LatLng {
        if (polyline.size < 2) return location

        val p = toWorld(location)
        var bestPoint = p
        var bestDist2 = Double.POSITIVE_INFINITY

        for (i in 0 until polyline.lastIndex) {
            val a = toWorld(polyline[i])
            val b = toWorld(polyline[i + 1])
            val proj = projectPointToSegment(p, a, b)
            val d2 = dist2(p, proj)
            if (d2 < bestDist2) {
                bestDist2 = d2
                bestPoint = proj
            }
        }

        // Convert "world" distance back to meters via local approximation around current latitude.
        val meters = worldDistanceToMeters(bestPoint, p, location.latitude)
        return if (meters <= maxSnapDistanceMeters) toLatLng(bestPoint) else location
    }

    private data class World(val x: Double, val y: Double)

    private fun toWorld(ll: LatLng): World {
        // WebMercator in "radians" space (unitless world)
        val x = ll.longitude * PI / 180.0
        val latRad = ll.latitude * PI / 180.0
        val y = ln(tan(PI / 4.0 + latRad / 2.0))
        return World(x, y)
    }

    private fun toLatLng(w: World): LatLng {
        val lng = w.x * 180.0 / PI
        val lat = (2.0 * atan(exp(w.y)) - PI / 2.0) * 180.0 / PI
        return LatLng(lat, lng)
    }

    private fun exp(v: Double) = kotlin.math.exp(v)

    private fun projectPointToSegment(p: World, a: World, b: World): World {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val apx = p.x - a.x
        val apy = p.y - a.y
        val ab2 = abx * abx + aby * aby
        if (ab2 == 0.0) return a
        val t = ((apx * abx) + (apy * aby)) / ab2
        val clamped = min(1.0, max(0.0, t))
        return World(a.x + abx * clamped, a.y + aby * clamped)
    }

    private fun dist2(p: World, q: World): Double {
        val dx = p.x - q.x
        val dy = p.y - q.y
        return dx * dx + dy * dy
    }

    private fun worldDistanceToMeters(a: World, b: World, atLatitudeDegrees: Double): Double {
        // Approx meters per radian at latitude for longitude; latitude scale is constant for radians.
        val metersPerRadLat = 6371000.0 // Earth radius (m)
        val metersPerRadLng = metersPerRadLat * cos(atLatitudeDegrees * PI / 180.0)
        val dx = abs(a.x - b.x) * metersPerRadLng
        val dy = abs(a.y - b.y) * metersPerRadLat
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}


