package com.avfusionapps.game_2048.shared.ui

import kotlin.math.PI

/** Replacements for java.lang.Math.toRadians / toDegrees in common code. */
internal fun toRadiansCompat(deg: Double): Double = deg * PI / 180.0
internal fun toDegreesCompat(rad: Double): Double = rad * 180.0 / PI
