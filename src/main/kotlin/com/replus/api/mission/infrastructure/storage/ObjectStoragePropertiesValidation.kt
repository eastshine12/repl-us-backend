package com.replus.api.mission.infrastructure.storage

import java.net.URI

internal fun ObjectStorageProperties.requireValidPublicBaseUrl() {
    val value = publicBaseUrl.trim()
    require(value.isNotBlank()) { "Object storage public base URL is required" }
    require(value.isHttpUri()) { "Object storage public base URL must be a valid HTTP(S) URI" }
}

internal fun ObjectStorageProperties.requireValidClientSettings() {
    require(region.trim().isNotBlank()) { "Object storage region is required" }

    endpoint.trim()
        .takeIf { it.isNotBlank() }
        ?.let {
            require(it.isHttpUri()) { "Object storage endpoint must be a valid HTTP(S) URI" }
        }
}

private fun String.isHttpUri(): Boolean =
    runCatching { URI.create(this) }
        .getOrNull()
        ?.let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
        ?: false
