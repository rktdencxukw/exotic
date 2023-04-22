package ai.platon.exotic.services.websocket

import java.security.Principal

class StompPrincipal(private val myName: String) : Principal {

    override fun getName(): String {
        return myName
    }
}