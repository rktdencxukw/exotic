package ai.platon.exotic.services.api.service

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class RuleService(
    private val ruleRepository: CrawlRuleRepository
) {
    private val cache = ConcurrentHashMap<Long, CrawlRule>()

    fun getRule(ruleId: Long): CrawlRule? {
        if (cache.contains(ruleId)) {
            return cache[ruleId]
        }
        val r = ruleRepository.findById(ruleId)
        return if (r.isPresent) {
            cache[ruleId] = r.get()
            r.get()
        } else {
            null
        }
    }

    fun cleanCache() {
        cache.clear()
    }

    fun kick(ruleId: Long) {
        cache.remove(ruleId)
    }
}