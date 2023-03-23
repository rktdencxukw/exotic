package ai.platon.exotic.services.api.controller.response

class ResponseBody<T> {
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
        fun error(s: String): ResponseBody<Any>? {
            return ResponseBody<Any>().apply {
                code = -1
                message = s
            }
        }

        fun ok(data: Any): ResponseBody<Any>? {
            return ResponseBody<Any>().apply {
                this.data = data
            }
        }
    }
}