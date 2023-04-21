package ai.platon.exotic.standalone.api

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*


@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api",
        "ai.platon.exotic.services.api",
        "ai.platon.exotic.services.module",
        "ai.platon.pulsar.driver.report",
        "ai.platon.pulsar.driver.scrape_node",
        "ai.platon.exotic.services.websocket",
    ],
    exclude = [EmbeddedMongoAutoConfiguration::class]
)
@ComponentScan(
    "ai.platon.scent.rest.api",
    "ai.platon.exotic.services.api",
    "ai.platon.exotic.standalone.api",
    "ai.platon.pulsar.driver.report", // 扫描 driver中http report controller
    "ai.platon.pulsar.driver.scrape_node",
    "ai.platon.exotic.services.module",
    "ai.platon.exotic.services.websocket",
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity",
    "ai.platon.exotic.services.module.market.entity",
    "ai.platon.exotic.services.module.trade.entity"
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableJpaRepositories(basePackages = ["ai.platon.exotic.services.api.persist", "ai.platon.exotic.services.module.market.persist", "ai.platon.exotic.services.module.trade.persist"])
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
// failed to import Applications
//@Import(ExoticApplication::class, ExoticServerApplication::class)
@EnableScheduling
@EnableJpaAuditing

class StandaloneApplication {
}

fun main(argv: Array<String>) {
//    TimeZone.setDefault(TimeZone.getTimeZone("GTM+8:00"))
    ExoticUtils.prepareDatabaseOrFail()
//    System.setProperty("scrape.submitter.dry.run", "true")
    SpringApplicationBuilder(StandaloneApplication::class.java)
        .profiles("h2")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*argv)
}
