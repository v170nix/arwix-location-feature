package net.arwix.location.common.extension

import kotlin.math.abs

private const val LOCATION_FORMAT_STRING = "%02d°%02d′%04.1f″"

private fun getDegree(double: Double): Triple<Int, Int, Double> {
    val degs = abs(double)
    val deg = degs.toInt()
    val minutes = (degs - deg) * 60.0
    val minute = minutes.toInt()
    val second = ((minutes - minute) * 60.0)
    return Triple(deg, minute, second)
}

fun latToString(latitude: Double, n: String, s: String): String {
    val ns = if (latitude > 0) n else s
    val lat = getDegree(latitude).toList().toTypedArray()
    return buildString {
        append(String.format(LOCATION_FORMAT_STRING, *lat))
        append(ns)
    }
}

fun lngToString(longitude: Double, e: String, w: String): String {
    return latToString(longitude, e, w)
}