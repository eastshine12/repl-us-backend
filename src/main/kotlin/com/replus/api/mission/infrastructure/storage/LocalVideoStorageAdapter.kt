package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.application.port.VideoUploadTarget
import com.replus.api.mission.application.port.VideoUploadVerification
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

    override fun verifyUploadedObject(
        objectKey: String,
        expectedContentType: String,
        expectedFileSizeBytes: Long,
    ): VideoUploadVerification =
        VideoUploadVerification(
            exists = true,
            contentType = expectedContentType,
            fileSizeBytes = expectedFileSizeBytes,
        )

    private fun appendObjectKey(baseUrl: String, objectKey: String): String =
        "${baseUrl.trimEnd('/')}/${objectKey.trimStart('/')}"
}
