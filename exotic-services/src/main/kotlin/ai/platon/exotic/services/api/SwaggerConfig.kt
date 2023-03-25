package ai.platon.exotic.services.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

/*
 sprint-data-rest接口也被swagger暴露了，scent-boot repository出现错误:
java.lang.IllegalStateException: Ambiguous search mapping detected. Both public abstract java.util.List ai.platon.scent.boot.autoconfigure.persist.CrawlSeedV3Repository.findAllByJobIdAndGroupAndPriorityAndStatusCode(java.lang.String,int,int,int) and public abstract org.springframework.data.domain.Page ai.platon.scent.boot.autoconfigure.    590 persist.CrawlSeedV3Repository.findAllByJobIdAndGroupAndPriorityAndStatusCode(java.lang.String,int,int,int,org.springframework.data.domain.PageRequest) are mapped to /findAllByJobIdAndGroupAndPriorityAndStatusCode! Tweak configuration to get to unambiguous paths!
 */
// 想去除spring-data-rest，pom.xml exlucsion + Docket.paths 都不管用。
// 放在这里成功禁止了spring-data-rest的暴露
// https://atchison.dev/disable-spring-data-rest-autoconfigure-from-springfox-boot-starter/
@Configuration
public class NoExposureRepositoryRestConfigurer : RepositoryRestConfigurer {
    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration, cors: CorsRegistry) {
        config.disableDefaultExposure()
    }
}

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    fun documentation(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("ai.platon.exotic.services.api.controller"))
            .paths(PathSelectors.any())
            .build()
            .pathMapping("/")
            .apiInfo(apiInfo())
            .enable(true)
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
            .title("API document")
            .description("Exotic api")
            .version("1.0")
            .build();
    }

}