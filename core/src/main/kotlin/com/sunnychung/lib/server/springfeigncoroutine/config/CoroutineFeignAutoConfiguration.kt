package com.sunnychung.lib.server.springfeigncoroutine.config

import com.sunnychung.lib.server.springfeigncoroutine.feign.SpringCoroutineContract
import feign.Contract
import feign.Logger
import feign.Retryer
import feign.kotlin.CoroutineFeign
import feign.micrometer.MicrometerCapability
import feign.micrometer.MicrometerObservationCapability
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.openfeign.DefaultFeignLoggerFactory
import org.springframework.cloud.openfeign.FeignClientProperties
import org.springframework.cloud.openfeign.FeignLoggerFactory
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CoroutineFeign::class)
@EnableConfigurationProperties(FeignClientProperties::class, FeignHttpClientProperties::class)
@AutoConfigureAfter(ObservationAutoConfiguration::class)
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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = ["spring.cloud.openfeign.micrometer.enabled"], matchIfMissing = true)
    @ConditionalOnClass(
        MicrometerObservationCapability::class,
        MicrometerCapability::class,
        MeterRegistry::class
    )
    protected class MicrometerConfiguration {
        @Bean
        @ConditionalOnMissingBean
//        @ConditionalOnBean(type = ["io.micrometer.observation.ObservationRegistry"])
        fun micrometerObservationCapability(registry: ObservationRegistry): MicrometerObservationCapability {
            return MicrometerObservationCapability(registry)
        }

        @Bean
//        @ConditionalOnBean(type = ["io.micrometer.core.instrument.MeterRegistry"])
        @ConditionalOnMissingBean(
            MicrometerCapability::class,
            MicrometerObservationCapability::class
        )
        fun micrometerCapability(registry: MeterRegistry): MicrometerCapability {
            return MicrometerCapability(registry)
        }
    }

    companion object {
        @JvmStatic
        @Bean
        @ConditionalOnMissingBean
        fun coroutineFeignClientRegistrar(environment: Environment, applicationContext: ApplicationContext): CoroutineFeignClientRegistrar {
            return CoroutineFeignClientRegistrar().also {
                it.setApplicationContext(applicationContext)
                it.setEnvironment(environment)
            }
        }
    }
}
