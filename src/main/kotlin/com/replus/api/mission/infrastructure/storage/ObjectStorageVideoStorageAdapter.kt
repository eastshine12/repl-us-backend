package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.application.port.VideoUploadTarget
import java.time.Instant

class ObjectStorageVideoStorageAdapter(
    private val properties: ObjectStorageProperties,
    private val uploadSigner: ObjectStorageUploadSigner,
) : VideoStoragePort {
    private val bucket = properties.bucket.trim()
    private val publicBaseUrl = properties.publicBaseUrl.trim()

    init {
        require(bucket.isNotBlank()) { "Object storage bucket is required" }
    }

    override fun createUploadTarget(
        objectKey: String,
        contentType: String,
        expiresAt: Instant,
        maxFileSizeBytes: Long,
    ): VideoUploadTarget {
        val presigned = uploadSigner.presignPutObject(
            PresignPutObjectCommand(
                bucket = bucket,
                objectKey = objectKey,
                contentType = contentType,
                expiresAt = expiresAt,
                maxFileSizeBytes = maxFileSizeBytes,
            ),
        )

        return VideoUploadTarget(
            uploadUrl = presigned.uploadUrl,
            method = "PUT",
            objectKey = objectKey,
            requiredHeaders = presigned.requiredHeaders + ("Content-Type" to contentType),
            expiresAt = expiresAt,
            maxFileSizeBytes = maxFileSizeBytes,
        )
    }

    override fun playbackUrl(objectKey: String): String =
        appendObjectKey(publicBaseUrl, objectKey)

    override fun thumbnailUrl(objectKey: String): String =
        appendObjectKey(publicBaseUrl, objectKey)

    private fun appendObjectKey(baseUrl: String, objectKey: String): String =
        "${baseUrl.trimEnd('/')}/${objectKey.trimStart('/')}"
}
