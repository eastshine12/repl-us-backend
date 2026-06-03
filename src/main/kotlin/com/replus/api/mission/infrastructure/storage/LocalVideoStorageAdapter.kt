package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.application.port.VideoUploadTarget
import java.time.Instant

class LocalVideoStorageAdapter(
    private val properties: LocalStorageProperties = LocalStorageProperties(),
) : VideoStoragePort {
    override fun createUploadTarget(
        objectKey: String,
        contentType: String,
        expiresAt: Instant,
        maxFileSizeBytes: Long,
    ): VideoUploadTarget =
        VideoUploadTarget(
            uploadUrl = appendObjectKey(properties.uploadBaseUrl, objectKey),
            method = "PUT",
            objectKey = objectKey,
            requiredHeaders = mapOf("Content-Type" to contentType),
            expiresAt = expiresAt,
            maxFileSizeBytes = maxFileSizeBytes,
        )

    override fun playbackUrl(objectKey: String): String =
        appendObjectKey(properties.playbackBaseUrl, objectKey)

    override fun thumbnailUrl(objectKey: String): String =
        appendObjectKey(properties.playbackBaseUrl, objectKey)

    private fun appendObjectKey(baseUrl: String, objectKey: String): String =
        "${baseUrl.trimEnd('/')}/${objectKey.trimStart('/')}"
}
