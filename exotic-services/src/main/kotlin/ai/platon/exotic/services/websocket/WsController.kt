package ai.platon.exotic.services.websocket

import ai.platon.exotic.services.module.scrape_node.entity.ScrapeNode
import ai.platon.pulsar.common.websocket.ScrapeNodeRegisterInfo
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.IpType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.util.HtmlUtils
import java.security.Principal
import java.util.*
import javax.annotation.PostConstruct


class HelloMessage {
    var name: String? = null

    constructor()
    constructor(name: String?) {
        this.name = name
    }
}
data class HelloMessage2(
    var name: String? = null
)
data class HelloMessage3(
    var name: String
)

class Greeting {
    var content: String? = null
        private set

    constructor()
    constructor(content: String?) {
        this.content = content
    }
}

@Controller
class WsController(
) {
    @PostConstruct
    fun init() {
        println("init WsController")
    }

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    @Throws(Exception::class)
    fun greeting(message: HelloMessage): Greeting {
        Thread.sleep(1000) // simulated delay
        return Greeting("Hello, " + HtmlUtils.htmlEscape(message.name!!) + "!")
    }

    @MessageMapping("/hello2")
    @SendTo("/topic/greetings")
    @Throws(Exception::class)
    fun greeting2(message: HelloMessage2): Greeting {
        Thread.sleep(1000) // simulated delay
        return Greeting("Hello, " + HtmlUtils.htmlEscape(message.name!!) + "!")
    }
}