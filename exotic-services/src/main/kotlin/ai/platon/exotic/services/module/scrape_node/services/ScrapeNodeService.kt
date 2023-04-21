package ai.platon.exotic.services.module.scrape_node.services

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.module.scrape_node.entity.ScrapeNode
import ai.platon.pulsar.persist.metadata.IpType
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

//@Service
class ScrapeNodeService {
    private val cache = ConcurrentHashMap<String, ScrapeNode>()

    fun getRule(ruleId: String): ScrapeNode? {
        if (cache.contains(ruleId)) {
            return cache[ruleId]
        } else {
            return null
        }
    }

    fun getNodesByIpType(ipType: IpType): List<ScrapeNode> {
        return cache.values.filter { it.ipType == ipType }
    }

    fun put(ruleId: String, node: ScrapeNode) {
        cache[ruleId] = node
    }

    fun cleanCache() {
        cache.clear()
    }

    fun kick(ruleId: String) {
        cache.remove(ruleId)
    }
}