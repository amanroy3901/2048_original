package com.avfusionapps.game_2048.shared.ui

/** "1,234,567" (Android: "%,d".format(n)). */
internal fun commas(n: Int): String {
    val neg = n < 0
    val digits = (if (neg) -n.toLong() else n.toLong()).toString()
    val sb = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return (if (neg) "-" else "") + sb.toString()
}

/** Zero-padded two digits (Android: "%02d"). */
internal fun pad2(n: Int): String = n.toString().padStart(2, '0')
