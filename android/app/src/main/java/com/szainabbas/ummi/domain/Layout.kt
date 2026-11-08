package com.szainabbas.ummi.domain

/** Small layout sums kept out of the composables so they can be tested on the JVM. */
object Layout {
    /** Side gap between the screen edge and Today's content, so no card or band runs edge to edge. */
    const val SCREEN_GUTTER_DP = 16

    /**
     * Āyah refs ("2:255") in Qurʾān order: by sūrah, then by āyah. Sorting
     * them as text put "2:255" after "25:74".
     */
    fun ayahOrder(refs: Collection<String>): List<String> =
        refs.sortedWith(compareBy({ it.part(0) }, { it.part(1) }, { it }))

    private fun String.part(i: Int): Int = split(':').getOrNull(i)?.toIntOrNull() ?: Int.MAX_VALUE

    /**
     * Splits [items] into rows of [perRow], padding the last row with nulls so
     * every chip takes the same share of the width and the grid lines up with
     * the card above it.
     */
    fun <T> gridRows(items: List<T>, perRow: Int): List<List<T?>> =
        items.chunked(perRow).map { row -> row + List(perRow - row.size) { null } }

    /**
     * Where a horizontal list has to start for item [index] to sit in the
     * middle of the viewport: the first visible item and how far it is
     * scrolled. [startPadding] is the list's content padding; everything is
     * in pixels. Items near the start can't be centred and stay at the left.
     */
    fun centredScroll(index: Int, itemPx: Int, gapPx: Int, startPadding: Int, viewportPx: Int): Pair<Int, Int> {
        val step = itemPx + gapPx
        val scroll = (startPadding + index * step + itemPx / 2 - viewportPx / 2).coerceAtLeast(0)
        return scroll / step to scroll % step
    }
}
