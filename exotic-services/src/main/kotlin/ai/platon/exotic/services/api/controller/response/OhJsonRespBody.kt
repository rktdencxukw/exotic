package ai.platon.exotic.services.api.controller.response

class OhJsonRespBody<T> {
    var code: Int = 0
    var message: String = "ok"
    var data: T? = null

    constructor(data: T) {
        this.data = data
    }

    constructor() {
        this.data = null as T
    }

    fun error(errMsg:String): OhJsonRespBody<T> {
        this.code = -1
        this.message = errMsg
        this.data = null
        return this
    }

    companion object {
        fun error(s: String): OhJsonRespBody<Any>? {
            return OhJsonRespBody<Any>().apply {
                code = -1
                message = s
            }
        }
        fun error(errMsg: String, data: Any): OhJsonRespBody<Any>? {
            return OhJsonRespBody<Any>().apply {
                this.code = -1
                this.message = errMsg
                this.data = data
            }
        }

        fun ok(): OhJsonRespBody<String> {
            return OhJsonRespBody<String>().apply {
                this.data = ""
            }
        }
        fun ok(data: Any): OhJsonRespBody<Any>? {
            return OhJsonRespBody<Any>().apply {
                this.data = data
            }
        }
    }
}