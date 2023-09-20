package com.sunnychung.lib.server.springfeigncoroutine.feign

import feign.MethodMetadata
import feign.kotlin.isSuspend
import feign.kotlin.kotlinMethodReturnType
import org.springframework.cloud.openfeign.support.SpringMvcContract
import java.lang.reflect.Method

class SpringCoroutineContract : SpringMvcContract() {

    override fun parseAndValidateMetadata(targetType: Class<*>, method: Method): MethodMetadata {
        val data = super.parseAndValidateMetadata(targetType, method)
        if (method.isSuspend) {
            data.returnType(method.kotlinMethodReturnType)
        }
        return data
    }
}
