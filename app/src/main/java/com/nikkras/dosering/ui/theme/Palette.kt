package com.nikkras.dosering.ui.theme

/** Ring colours. Mid-tones that stay readable on both light and dark home screens. */
object Palette {

    val colors: List<Int> = listOf(
        0xFF14B8A6, // teal
        0xFFF25F5C, // coral
        0xFFF59E0B, // amber
        0xFF3B82F6, // blue
        0xFF8B5CF6, // violet
        0xFFEC4899, // pink
        0xFF22C55E, // green
        0xFFF97316, // orange
    ).map { it.toInt() }

    /** First colour not used yet, so new medicines are easy to tell apart. */
    fun next(used: Collection<Int>): Int = colors.firstOrNull { it !in used } ?: colors[used.size % colors.size]
}
