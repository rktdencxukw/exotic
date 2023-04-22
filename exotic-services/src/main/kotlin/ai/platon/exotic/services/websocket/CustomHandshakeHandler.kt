package ai.platon.exotic.services.websocket

import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.*

class CustomHandshakeHandler(strategy: TomcatRequestUpgradeStrategy) :
    DefaultHandshakeHandler(strategy) {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal? {
        return StompPrincipal(UUID.randomUUID().toString())
    }
}