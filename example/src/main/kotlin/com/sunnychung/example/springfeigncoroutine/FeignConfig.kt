package com.sunnychung.example.springfeigncoroutine

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {

    @Bean
    fun decoder() = JacksonDecoder(jacksonObjectMapper())

    @Bean
    fun encoder() = JacksonEncoder()
}
