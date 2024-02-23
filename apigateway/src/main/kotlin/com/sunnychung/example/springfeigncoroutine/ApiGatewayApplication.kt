package com.sunnychung.example.springfeigncoroutine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {
	System.setProperty("reactor.netty.http.server.accessLogEnabled", "true")
	Hooks.enableAutomaticContextPropagation()
	runApplication<ApiGatewayApplication>(*args)
}
