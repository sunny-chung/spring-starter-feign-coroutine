package com.sunnychung.lib.server.springfeigncoroutine.config

import com.sunnychung.lib.server.springfeigncoroutine.annotation.CoroutineFeignClient
import com.sunnychung.lib.server.springfeigncoroutine.annotation.EnableCoroutineFeignClients
import com.sunnychung.lib.server.springfeigncoroutine.feign.AddHeaderFeignRequestInterceptor
import feign.Contract
import feign.Logger
import feign.Request
import feign.Retryer
import feign.codec.Decoder
import feign.codec.Encoder
import feign.kotlin.CoroutineFeign
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.cloud.openfeign.FeignClientProperties
import org.springframework.cloud.openfeign.FeignLoggerFactory
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.concurrent.TimeUnit

class CoroutineFeignClientRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {

    lateinit var feignClientProperties: FeignClientProperties

    override fun setEnvironment(environment: Environment) {
        feignClientProperties = Binder.get(environment)
            .bind("spring.cloud.openfeign.client", FeignClientProperties::class.java)
            .orElseThrow { IllegalStateException("Cannot bind FeignClientProperties") }

        println("Binded FeignClientProperties")
    }

    override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        println("${javaClass.simpleName} ImportBeanDefinitionRegistrar r=${registry.javaClass.name}")

        val attributes = importingClassMetadata.getAnnotationAttributes(EnableCoroutineFeignClients::class.qualifiedName!!, true)!!
        val basePackages = attributes["basePackages"] as Array<String>
        println("bp = ${basePackages.contentToString()}")

        val beanRegistry = registry as ConfigurableListableBeanFactory

//        val feignClientProperties = beanRegistry.getBean(FeignClientProperties::class.java)
//        val decoder: Decoder = beanRegistry.getBean(Decoder::class.java)
//        val encoder: Encoder = beanRegistry.getBean(Encoder::class.java)
//        val contract: Contract = beanRegistry.getBean(Contract::class.java)

        val scanProvider = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return super.isCandidateComponent(beanDefinition) || beanDefinition.metadata.isAbstract
            }
        }.apply {
            addIncludeFilter(AnnotationTypeFilter(CoroutineFeignClient::class.java))
        }

        basePackages.flatMap { scanProvider.findCandidateComponents(it) }
            .map { it.beanClassName }
            .distinct()
            .forEach {
                val clazz = Class.forName(it)
                val annotation = clazz.getAnnotation(CoroutineFeignClient::class.java)

                val config = feignClientProperties.config[annotation.name] ?:
                    feignClientProperties.config[feignClientProperties.defaultConfig]!!

                // TODO copy default config to specific config if partially null

                beanRegistry.registerSingleton(
                    clazz.simpleName,
                    createCoroutineFeignClient(beanRegistry, config, clazz)
                )
                println("Registered ${clazz.name} ${annotation.name}")
            }
    }

    fun <T> createCoroutineFeignClient(beanRegistry: ConfigurableListableBeanFactory, config: FeignClientProperties.FeignClientConfiguration, type: Class<T>): T {
        val builder = CoroutineFeign.builder<Unit>()
            .requestInterceptors(config.requestInterceptors?.map { beanRegistry.getBean(it) } ?: listOf())
            .logLevel(config.loggerLevel ?: Logger.Level.NONE)
            .contract(beanRegistry.getBean(config.contract ?: Contract::class.java))
            .decoder(beanRegistry.getBean(config.decoder ?: Decoder::class.java))
            .encoder(beanRegistry.getBean(config.encoder ?: Encoder::class.java))
            .retryer(beanRegistry.getBean(config.retryer ?: Retryer::class.java))
            .logger(beanRegistry.getBean(FeignLoggerFactory::class.java).create(type))

        config.queryMapEncoder?.let { builder.queryMapEncoder(beanRegistry.getBean(it)) }
        config.errorDecoder?.let { builder.errorDecoder(beanRegistry.getBean(it)) }

        if (config.dismiss404 == true) { builder.dismiss404() }
        config.exceptionPropagationPolicy?.let { builder.exceptionPropagationPolicy(it) }
        config.capabilities?.forEach { builder.addCapability(beanRegistry.getBean(it)) }

        builder.options(Request.Options(
            /* connectTimeout = */ config.connectTimeout?.toLong() ?: (10 * 1000L),
            /* connectTimeoutUnit = */ TimeUnit.MILLISECONDS,
            /* readTimeout = */ config.readTimeout?.toLong() ?: (30 * 1000L),
            /* readTimeoutUnit = */ TimeUnit.MILLISECONDS,
            /* followRedirects = */ config.isFollowRedirects ?: true
        ))

        config.defaultRequestHeaders.orEmpty().let {
            if (it.isNotEmpty()) {
                builder.requestInterceptor(AddHeaderFeignRequestInterceptor(it))
            }
        }

        // TODO default query parameters

        return builder.target(type, config.url)
    }
}
