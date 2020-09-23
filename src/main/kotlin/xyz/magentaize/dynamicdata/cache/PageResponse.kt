package xyz.magentaize.dynamicdata.cache

interface PageResponse {
    val pageSize: Int
    val page: Int
    val pages: Int
    val totalSize: Int
}
