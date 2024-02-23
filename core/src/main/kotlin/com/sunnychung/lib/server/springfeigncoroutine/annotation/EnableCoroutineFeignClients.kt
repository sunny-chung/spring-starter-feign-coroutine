package com.sunnychung.lib.server.springfeigncoroutine.annotation

import com.sunnychung.lib.server.springfeigncoroutine.config.CoroutineFeignAutoConfiguration
import com.sunnychung.lib.server.springfeigncoroutine.config.CoroutineFeignClientRegistrar
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(CoroutineFeignAutoConfiguration::class/*, CoroutineFeignClientRegistrar::class*/)
annotation class EnableCoroutineFeignClients(
    val basePackages: Array<String>
)
