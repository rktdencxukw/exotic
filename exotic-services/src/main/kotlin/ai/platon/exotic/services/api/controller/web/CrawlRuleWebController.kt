package ai.platon.exotic.services.api.controller.web

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.controller.response.ResponseBody
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.common.jackson.prettyScentObjectWritter
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.getLogger
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.stream.Collectors
import javax.validation.Valid

@Controller
@RequestMapping("crawl/rules")
class CrawlRuleWebController(
    private val repository: CrawlRuleRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
) {
    private val amazonSeeds = LinkExtractors.fromResource("sites/amazon/best-sellers.txt")
    private val amazonItemSQLTemplate = ResourceLoader.readString("sites/amazon/sqls/x-item.sql").trim()
    private val sqlTemplate = """
select
  dom_all_attrs(dom, 'a.news-flash-item-title', 'href') as ids,
  dom_all_texts(dom, '.news-flash-item-title') as titles,
  dom_all_texts(dom, '.news-flash-item-content') as contents
from load_and_select('{{url}}', 'body');
    """

    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        model: Model
    ): String {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val rules = repository.findAllByStatusNot(RuleStatus.Archived.toString(), pageable)
        model.addAttribute("rules", rules)
        return "crawl/rules/index"
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: Long, model: Model): String {
        val rule = repository.getById(id)

        model.addAttribute("rule", rule)
        val tasks = rule.portalTasks.sortedByDescending { it.id }
        model.addAttribute("tasks", tasks)

        return "crawl/rules/view"
    }

//    @GetMapping("/add")
//    fun showAddForm(model: Model): String {
//        val rule = CrawlRule()
//        rule.sqlTemplate = amazonItemSQLTemplate
//
//        val n = 2 + Random.nextInt(4)
//        rule.portalUrls = amazonSeeds.shuffled().take(n).joinToString("\n")
//        rule.outLinkSelector = "a[href~=/dp/]"
//        rule.nextPageSelector = "ul.a-pagination li.a-last a"
//
//        model.addAttribute("rule", rule)
//
//        return "crawl/rules/add"
//    }

    @GetMapping("/add")
    fun create(model: Model): String {
        //        getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))
        val rule = CrawlRule()
        rule.id = 0L
        rule.type = RuleType.Entity.toString()
        rule.sqlTemplate = sqlTemplate
        model.addAttribute("rule", rule)
        model.addAttribute("id", 0L)
        return "crawl/rules/edit"
    }

    @GetMapping("/jd/add")
    fun showJdAddForm(model: Model): String {
        val rule = CrawlRule()
        model.addAttribute("rule", rule)
        return "crawl/rules/jd/add"
    }

    @PostMapping("/add")
    fun add(@Valid @ModelAttribute("rule") rule: CrawlRule, result: BindingResult, model: Model): String {
        getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))

        if (result.hasErrors()) {
            // model.addAttribute("rule", rule)
            return "crawl/rules/add"
        }

        rule.createdDate = Instant.now()
        rule.status = RuleStatus.Created.toString()
        if (!rule.period.isNegative) { // 没用 cron
            rule.cronExpression = ""
        }
        if (rule.type == RuleType.Entity.toString()) {
            rule.outLinkSelector = ""
        }

        repository.save(rule)
        return "redirect:/crawl/rules/"
    }

    @PostMapping("/test_run")
    fun testRun(
        @Valid @RequestBody rule: CrawlRule,
        errors: Errors
    ): ResponseEntity<ResponseBody<Any>> {
        getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))

        //If error, just return a 400 bad request, along with the error message
        if (errors.hasErrors()) {
            val msg = errors.allErrors
                .stream().map { x -> x.defaultMessage }
                .collect(Collectors.joining(","))
            return ResponseEntity.badRequest().body(ResponseBody.error(msg))
        }
        // TODO not retry
        var portalTask = PortalTask(rule.portalUrls, "", 3)
        var scrapeTask = ScrapeTask(rule.portalUrls, "", 3, rule.sqlTemplate!!)
        scrapeTask.companionPortalTask = portalTask

        val listenableScrapeTask = ListenableScrapeTask(scrapeTask).also {
            it.onSubmitted = {
                it.task.status = TaskStatus.SUBMITTED }
            it.onRetry = {
                it.task.status = TaskStatus.RETRYING }
            it.onSuccess = {
                it.task.status = TaskStatus.OK
            }
            it.onFailed = {
                it.task.status = TaskStatus.FAILED }
            it.onFinished = {
                it.task.status = TaskStatus.OK }
            it.onTimeout = {
                it.task.status = TaskStatus.FAILED }
        }

        val taskSubmitter = TaskSubmitter(exoticCrawler.driverSettings)
        taskSubmitter.scrape(listenableScrapeTask)

        while (scrapeTask.status != TaskStatus.OK || scrapeTask.status != TaskStatus.FAILED) {
            // sleep 1s
            Thread.sleep(1000)
        }

        return ResponseEntity.ok(ResponseBody.ok(scrapeTask))
    }

    @GetMapping("/edit/{id}")
    fun edit(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        model.addAttribute("rule", rule)
        return "crawl/rules/edit"
    }

    @PostMapping("update/{id}")
    fun update(
        @PathVariable("id") id: Long, @Valid rule: CrawlRule, result: BindingResult,
        model: Model
    ): String? {
        if (result.hasErrors()) {
            return "crawl/rules/edit"
        }
        if (id == 0L) {
            rule.createdDate = Instant.now()
            rule.status = RuleStatus.Created.toString()
        } else {
            val old = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
            rule.status = old.status
            rule.crawlCount = old.crawlCount
            rule.createdDate = old.createdDate
            rule.lastCrawlTime = old.lastCrawlTime
            rule.crawlHistory = old.crawlHistory
            rule.idsOfLast = old.idsOfLast
            rule.type = old.type
        }

        if (!rule.period.isNegative) { // 没用 cron
            rule.cronExpression = ""
        }

        if (rule.type == RuleType.Entity.toString()) {
            rule.outLinkSelector = ""
        }

        val ruleT = repository.save(rule)

        return "redirect:/crawl/rules/view/${ruleT.id}"
    }

    @GetMapping("pause/{id}")
    fun pause(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Paused.toString()
        repository.save(rule)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("start/{id}")
    fun start(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Created.toString()
//        rule.adjustFields()
        repository.save(rule)

        crawlTaskRunner.startCrawl(rule)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("admin/")
    fun adminList(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        model: Model
    ): String {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val rules = repository.findAll(pageable)
        model.addAttribute("rules", rules)
        return "crawl/rules/admin/index"
    }

    @GetMapping("admin/archive/{id}")
    fun adminArchive(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Archived.toString()
//        rule.adjustFields()
        repository.save(rule)

        return "redirect:/crawl/rules/admin/"
    }
}
