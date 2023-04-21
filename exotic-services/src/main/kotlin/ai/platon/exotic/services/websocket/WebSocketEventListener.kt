package ai.platon.exotic.services.websocket

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

//@Component
class WebSocketEventListener {
    @Autowired
    private val messagingTemplate: SimpMessageSendingOperations? = null
    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent?) {
        WebSocketEventListener.Companion.logger.info("Received a new web socket connection")
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val username = headerAccessor.sessionAttributes!!["username"] as String?
        if (username != null) {
//            WebSocketEventListener.Companion.logger.info("User Disconnected : $username")
//            val chatMessage = ChatMessage()
//            chatMessage.setType(ChatMessage.MessageType.LEAVE)
//            chatMessage.setSender(username)
//            messagingTemplate.convertAndSend("/topic/public", chatMessage)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)
    }
}