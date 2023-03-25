package ai.platon.exotic.services.api.controller.response

class OhJsonRespBody<T> {
    var code: Int = 0
    var message: String = "ok"
    var data: T? = null

    constructor(data: T) {
        this.data = data
    }

    constructor() {
        this.data = "" as T
    }


    companion object {
        fun error(s: String): OhJsonRespBody<Any>? {
            return OhJsonRespBody<Any>().apply {
                code = -1
                message = s
            }
        }

        fun ok(): OhJsonRespBody<Any>? {
            return OhJsonRespBody<Any>().apply {
                this.data = "" as Any
            }
        }
        fun ok(data: Any): OhJsonRespBody<Any>? {
            return OhJsonRespBody<Any>().apply {
                this.data = data
            }
        }
    }
}