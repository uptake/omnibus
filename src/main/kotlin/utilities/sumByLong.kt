package utilities

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    return this.map { selector(it) }.fold(0L) { t, n -> t + n }
}
