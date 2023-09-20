package com.sunnychung.lib.server.springfeigncoroutine.feign

import feign.RequestInterceptor
import feign.RequestTemplate

internal class AddHeaderFeignRequestInterceptor(val headers: Map<String, Collection<String>>) : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        headers.forEach {
            template.header(it.key, it.value)
        }
    }
}
