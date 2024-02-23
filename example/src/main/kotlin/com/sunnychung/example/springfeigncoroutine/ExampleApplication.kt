package com.sunnychung.example.springfeigncoroutine

import com.sunnychung.lib.server.springfeigncoroutine.annotation.EnableCoroutineFeignClients
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
@EnableCoroutineFeignClients(basePackages = ["com.sunnychung.example.springfeigncoroutine"])
class ExampleApplication

fun main(args: Array<String>) {
	System.setProperty("reactor.netty.http.server.accessLogEnabled", "true")
	Hooks.enableAutomaticContextPropagation()
	runApplication<ExampleApplication>(*args)
}
