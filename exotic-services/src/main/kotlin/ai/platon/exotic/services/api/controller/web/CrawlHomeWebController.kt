package ai.platon.exotic.services.api.controller.web

import org.springframework.web.bind.annotation.GetMapping

//@Controller
//@RequestMapping("crawl")
class CrawlHomeWebController {
    @GetMapping("/")
    fun home(): String {
        return "crawl/home"
    }
}
