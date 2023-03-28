package ai.platon.exotic.services.api.component

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*


/**
 * @Description 解决springboot高版本下日期转json时jackson方式不生效问题
 *
# enable @EnableWebMvc 使用mvc拦截器后这里无效
 *
 */
@Configuration
public class DateFormatForJson : WebMvcConfigurer {
    /**
     * 使用此方法, 以下 spring-boot: jackson时间格式化 配置 将会失效
     * spring.jackson.time-zone=GMT+8
     * spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
     * 原因: 会覆盖 @EnableAutoConfiguration 关于 WebMvcAutoConfiguration 的配置
     * */
    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        var converter = MappingJackson2HttpMessageConverter();
        var objectMapper = converter.objectMapper;
        // 生成JSON时,将所有Long转换成String
//        var simpleModule = SimpleModule();
//        objectMapper.registerModule(simpleModule);
        // 时间格式化
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//        objectMapper.setDateFormat(SimpleDateFormat("yyyy-MM-ddTHH:mm:ss.SSS[Z]"));
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        // 设置格式化内容
        converter.objectMapper = objectMapper;
        converters.add(0, converter);
    }
}