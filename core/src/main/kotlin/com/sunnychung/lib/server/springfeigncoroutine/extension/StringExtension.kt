package com.sunnychung.lib.server.springfeigncoroutine.extension

internal fun String?.emptyToNull(): String? {
    return if (isNullOrEmpty()) {
        null
    } else {
        this
    }
}
