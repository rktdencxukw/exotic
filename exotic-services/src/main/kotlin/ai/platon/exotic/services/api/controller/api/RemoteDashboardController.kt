package ai.platon.exotic.services.api.controller.api

import ai.platon.exotic.driver.crawl.ExoticCrawler
import com.google.gson.GsonBuilder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/crawl/remote/dashboard")
class RemoteDashboardController(
    private val exoticCrawler: ExoticCrawler,
) {
    private val driver get() = exoticCrawler.driver

    @GetMapping
    fun dashboard(model: Model): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val dashboard = driver.dashboard()
        model.addAttribute("dashboard", gson.toJson(dashboard))
        return "crawl/remote/tasks/dashboard"
    }
}
