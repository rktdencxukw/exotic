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
    @MessageMapping("/hello3")
    @SendTo("/topic/greetings")
    @Throws(Exception::class)
    fun greeting2(message: HelloMessage3): Greeting {
        Thread.sleep(1000) // simulated delay
        return Greeting("Hello, " + HtmlUtils.htmlEscape(message.name!!) + "!")
    }

    @MessageMapping("/test")
    @SendTo("/topic/greetings")
    @Throws(Exception::class)
    fun test(@Payload message: String): String {
        Thread.sleep(1000) // simulated delay
        return "Heowjfo"
    }

    @MessageMapping("/scrape6")
    @SendTo("/topic/greetings")
    fun register6(
        @Payload message: ScrapeNodeRegisterInfo,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        return scrapeNode.nodeId
    }
    @MessageMapping("/scrape_register_test5")
    @SendTo("/topic/greetings")
    fun register5(
        message: HelloMessage2,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        return "test2"
    }

    @MessageMapping("/scrape_register_test4")
    @SendTo("/topic/greetings")
    fun register4(
        @Payload message: ScrapeNodeRegisterInfo,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        return scrapeNode.nodeId
    }

    @MessageMapping("/scrape_register_test3")
    @SendToUser("/queue/scrape_register")
    fun register3(
        @Payload message: ScrapeNodeRegisterInfo,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        return scrapeNode.nodeId
    }

    @MessageMapping("/scrape_register_test21")
    @SendToUser("/queue/scrape_register")
    fun register21(
        @Payload message: ScrapeNodeRegisterInfo,
        headerAccessor: SimpMessageHeaderAccessor,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        scrapeNode.wsSessionId = headerAccessor.sessionId!!
        return scrapeNode.nodeId
    }

    @MessageMapping("/scrape_register_test2")
    @SendToUser("/queue/scrape_register")
    fun register2(
        @Payload message: ScrapeNodeRegisterInfo,
        headerAccessor: SimpMessageHeaderAccessor,
        user: Principal?,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        scrapeNode.wsSessionId = headerAccessor.sessionId!!
        return scrapeNode.nodeId
    }
    @MessageMapping("/scrape_register_test1")
    @SendToUser("/queue/scrape_register")
    fun register(
        @Payload message: ScrapeNodeRegisterInfo,
        user: Principal?,
    ): String? {
        var scrapeNode = ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        return scrapeNode.nodeId
    }
}