package ai.platon.exotic.services.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy
import javax.annotation.PostConstruct


@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    @PostConstruct
    fun init() {
        println("init WebSocketConfig")
    }
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        var strategy = TomcatRequestUpgradeStrategy()

//        registry.addEndpoint("/ws")
//            .setHandshakeHandler(CustomHandshakeHandler(strategy))
//            .setAllowedOriginPatterns("*")
//            .withSockJS().setSessionCookieNeeded(false)
//            .setStreamBytesLimit(512 * 1024)
//            .setHttpMessageCacheSize(1000)
//            .setDisconnectDelay(30 * 1000)
        registry.addEndpoint("/ws")
            .setHandshakeHandler(CustomHandshakeHandler(strategy))
            .setAllowedOriginPatterns("*")
//            .withSockJS() 带sockjs会链接失败，原因未知. 好像是阻止跨域和http升级到ws了


        // 可以这样设置两组？
//        registry.addEndpoint("/socket").setAllowedOriginPatterns("*");
//        registry.addEndpoint("/socket").setAllowedOriginPatterns("*").withSockJS();
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic/", "/queue/")
        config.setApplicationDestinationPrefixes("/app")
    }
}