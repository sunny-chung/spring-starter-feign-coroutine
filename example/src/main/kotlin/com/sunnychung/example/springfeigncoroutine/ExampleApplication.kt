package com.sunnychung.example.springfeigncoroutine

import com.sunnychung.lib.server.springfeigncoroutine.annotation.EnableCoroutineFeignClients
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableCoroutineFeignClients(basePackages = ["com.sunnychung.example.springfeigncoroutine"])
class ExampleApplication

fun main(args: Array<String>) {
	runApplication<ExampleApplication>(*args)
}
