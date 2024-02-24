package com.sunnychung.lib.server.springfeigncoroutine.config

import com.sunnychung.lib.server.springfeigncoroutine.annotation.CoroutineFeignClient
import com.sunnychung.lib.server.springfeigncoroutine.annotation.EnableCoroutineFeignClients
import com.sunnychung.lib.server.springfeigncoroutine.extension.emptyToNull
import com.sunnychung.lib.server.springfeigncoroutine.feign.AddHeaderFeignRequestInterceptor
import com.sunnychung.lib.server.springfeigncoroutine.httpclient.webclient.WebClientExecutor
import com.sunnychung.lib.server.springfeigncoroutine.mapper.ConfigMapper
import feign.Capability
import feign.ContextManipulateProvider
import feign.Contract
import feign.Logger
import feign.Request
import feign.Retryer
import feign.codec.Decoder
import feign.codec.Encoder
import feign.kotlin.CoroutineFeign
import io.micrometer.context.ContextSnapshot
import io.micrometer.context.ContextSnapshotFactory
import io.netty.channel.ChannelOption
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.cloud.openfeign.FeignClientProperties
import org.springframework.cloud.openfeign.FeignLoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.Environment
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

class CoroutineFeignClientRegistrar : BeanDefinitionRegistryPostProcessor/*, EnvironmentAware, ApplicationContextAware*/ {

    lateinit var feignClientProperties: FeignClientProperties

    private lateinit var applicationContext: ApplicationContext

    fun setEnvironment(environment: Environment) {
        feignClientProperties = Binder.get(environment)
            .bind("spring.cloud.openfeign.client", FeignClientProperties::class.java)
            .orElseThrow { IllegalStateException("Cannot bind FeignClientProperties") }

        println("Binded FeignClientProperties")
    }

    fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    fun <T> createCoroutineFeignClient(context: ApplicationContext, config: FeignClientProperties.FeignClientConfiguration, type: Class<T>): T {
        println("Building FeignClient ${type.name}")
        fun <T> getBean(clazz: Class<T>): T {
            return context.getBean(clazz)
        }

        val builder = CoroutineFeign.builder<Unit>()
            .requestInterceptors(config.requestInterceptors?.map { getBean(it) } ?: listOf())
            .logLevel(config.loggerLevel ?: Logger.Level.NONE)
            .contract(getBean(config.contract ?: Contract::class.java))
            .decoder(getBean(config.decoder ?: Decoder::class.java))
            .encoder(getBean(config.encoder ?: Encoder::class.java))
            .retryer(getBean(config.retryer ?: Retryer::class.java))
            .logger(getBean(FeignLoggerFactory::class.java).create(type))

        config.responseInterceptor?.let { builder.responseInterceptor(getBean(it)) }

        config.queryMapEncoder?.let { builder.queryMapEncoder(getBean(it)) }
        config.errorDecoder?.let { builder.errorDecoder(getBean(it)) }

        if (config.dismiss404 == true) { builder.dismiss404() }
        config.exceptionPropagationPolicy?.let { builder.exceptionPropagationPolicy(it) }
        config.capabilities?.forEach { builder.addCapability(getBean(it).also { println(">> addCapability1 $it") }) }
        if (config.capabilities.isNullOrEmpty()) {
            try {
                getBean(Capability::class.java)?.also {
                    println("addCapability2 $it")
                    builder.addCapability(it)
                }
            } catch (_: NoSuchBeanDefinitionException) {}
        }

        val connectTimeoutMs = config.connectTimeout?.toLong() ?: (10 * 1000L)
        val isFollowRedirects = config.isFollowRedirects ?: true
        builder.options(Request.Options(
            /* connectTimeout = */ connectTimeoutMs,
            /* connectTimeoutUnit = */ TimeUnit.MILLISECONDS,
            /* readTimeout = */ config.readTimeout?.toLong() ?: (30 * 1000L),
            /* readTimeoutUnit = */ TimeUnit.MILLISECONDS,
            /* followRedirects = */ isFollowRedirects
        ))

        config.defaultRequestHeaders.orEmpty().let {
            if (it.isNotEmpty()) {
                builder.requestInterceptor(AddHeaderFeignRequestInterceptor(it))
            }
        }

        // TODO default query parameters

        builder.client(WebClientExecutor {
            val connectionProvider = ConnectionProvider.builder("webclient-connections")
                .maxConnections(2000)
                .pendingAcquireMaxCount(6100)
                .maxIdleTime(Duration.ofSeconds(10))
                .build()
            val httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs.toInt())
                .followRedirect(isFollowRedirects)
            clientConnector(ReactorClientHttpConnector(httpClient))
        })

        builder.contextManipulateProvider(object : ContextManipulateProvider {
            val CONTEXT_KEY = "MicrometerContext"

            override fun snapshot(context: MutableMap<String, Any>) {
                val snapshot: ContextSnapshot = ContextSnapshotFactory.builder().build().captureAll()
                context[CONTEXT_KEY] = snapshot
            }

            override fun restore(context: MutableMap<String, Any>) {
                (context[CONTEXT_KEY] as ContextSnapshot).setThreadLocals()
            }
        })

        return builder.target(type, config.url)
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        // noop
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val annotatedConfigBean = applicationContext.getBeansWithAnnotation(EnableCoroutineFeignClients::class.java).values.singleOrNull() ?: return
        val annotation = annotatedConfigBean.javaClass.getAnnotation(EnableCoroutineFeignClients::class.java)
        val basePackages = annotation.basePackages

        val scanProvider = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return super.isCandidateComponent(beanDefinition) || beanDefinition.metadata.isAbstract
            }
        }.apply {
            addIncludeFilter(AnnotationTypeFilter(CoroutineFeignClient::class.java))
        }

        val configMapper = ConfigMapper.INSTANCE

        basePackages.flatMap { scanProvider.findCandidateComponents(it) }
            .map { it.beanClassName }
            .distinct()
            .forEach {
                val clazz = Class.forName(it)
                val annotation = clazz.getAnnotation(CoroutineFeignClient::class.java)

                val config = FeignClientProperties.FeignClientConfiguration()
                val propertiesDefaultConfig = feignClientProperties.config[feignClientProperties.defaultConfig]
                val propertiesSpecificConfig = feignClientProperties.config[annotation.name]
                configMapper.copy(from = propertiesDefaultConfig, to = config)
                configMapper.copy(from = propertiesSpecificConfig, to = config)
                annotation.url.emptyToNull()?.let { config.url = it }

                registry.registerBeanDefinition(
                    clazz.simpleName,
                    GenericBeanDefinition().also {
                        it.setBeanClass(clazz)
                        it.setInstanceSupplier { createCoroutineFeignClient(applicationContext, config, clazz) }
                    }

                )
                println("Registered ${clazz.name} ${annotation.name}")
            }
    }
}
