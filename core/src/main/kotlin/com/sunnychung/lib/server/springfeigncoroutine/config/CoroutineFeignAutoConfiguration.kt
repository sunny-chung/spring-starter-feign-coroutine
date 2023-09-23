package com.sunnychung.lib.server.springfeigncoroutine.config

import com.sunnychung.lib.server.springfeigncoroutine.feign.SpringCoroutineContract
import feign.Contract
import feign.Logger
import feign.Retryer
import feign.kotlin.CoroutineFeign
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.openfeign.DefaultFeignLoggerFactory
import org.springframework.cloud.openfeign.FeignClientProperties
import org.springframework.cloud.openfeign.FeignLoggerFactory
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CoroutineFeign::class)
@EnableConfigurationProperties(FeignClientProperties::class, FeignHttpClientProperties::class)
class CoroutineFeignAutoConfiguration {

    @Autowired(required = false)
    var logger: Logger? = null

    @Bean
    @ConditionalOnMissingBean(Contract::class)
    fun contract(): Contract = SpringCoroutineContract()

    @Bean
    @ConditionalOnMissingBean(Retryer::class)
    fun feignRetryer(): Retryer {
        return Retryer.NEVER_RETRY
    }

    @Bean
    @ConditionalOnMissingBean(FeignLoggerFactory::class)
    fun feignLoggerFactory(): FeignLoggerFactory {
        return DefaultFeignLoggerFactory(logger)
    }
}
