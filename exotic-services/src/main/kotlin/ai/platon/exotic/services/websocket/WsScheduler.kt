package ai.platon.exotic.services.websocket

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import javax.annotation.Resource

@EnableScheduling
@Component
public class WsScheduler(
    @Resource
    private val simpMessagingTemplate: SimpMessagingTemplate
) {



//    @Scheduled(cron = "0/5 * * * * ?")
    public fun start()
    {
//        RequestMsg requestMsg = new RequestMsg();
//        requestMsg.setBody(LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")));
//        String jsonString = JSONObject . toJSONString (requestMsg);
        simpMessagingTemplate.convertAndSend("/topic/result_feed", "hi " + Date())
    }
}