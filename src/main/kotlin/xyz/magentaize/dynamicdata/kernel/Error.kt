package xyz.magentaize.dynamicdata.kernel

data class Error<K, V>(
    val exception: Exception,
    val key: K,
    val value: V
) {
}