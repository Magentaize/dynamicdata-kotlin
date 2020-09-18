package dynamicdata.cache

data class AnonymousPageRequest (
    override val page: Int = 1,
    override val size: Int = 25
) : PageRequest {
    companion object{
        val default = AnonymousPageRequest()
        val empty = AnonymousPageRequest(0, 0)
    }

    init {
        require(page >= 0){
            "page must be positive"
        }

        require(size >= 0){
            "size must be positive"
        }
    }
}
