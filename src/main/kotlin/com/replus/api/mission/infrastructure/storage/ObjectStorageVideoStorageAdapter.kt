package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.application.port.VideoUploadTarget
import com.replus.api.mission.application.port.VideoUploadVerification
import java.time.Instant

class ObjectStorageVideoStorageAdapter(
    private val properties: ObjectStorageProperties,
    private val uploadSigner: ObjectStorageUploadSigner,
    private val uploadVerifier: ObjectStorageUploadVerifier,
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
            requiredHeaders = presigned.requiredHeaders.withoutContentType() + ("Content-Type" to contentType),
            expiresAt = expiresAt,
            maxFileSizeBytes = maxFileSizeBytes,
        )
    }

    override fun playbackUrl(objectKey: String): String =
        appendObjectKey(publicBaseUrl, objectKey)

    override fun thumbnailUrl(objectKey: String): String =
        appendObjectKey(publicBaseUrl, objectKey)

    override fun verifyUploadedObject(
        objectKey: String,
        expectedContentType: String,
        expectedFileSizeBytes: Long,
    ): VideoUploadVerification {
        val verified = uploadVerifier.verifyUploadedObject(
            VerifyUploadedObjectCommand(
                bucket = bucket,
                objectKey = objectKey,
                expectedContentType = expectedContentType,
                expectedFileSizeBytes = expectedFileSizeBytes,
            ),
        )

        return VideoUploadVerification(
            exists = verified.exists,
            contentType = verified.contentType,
            fileSizeBytes = verified.fileSizeBytes,
        )
    }

    private fun appendObjectKey(baseUrl: String, objectKey: String): String =
        "${baseUrl.trimEnd('/')}/${objectKey.trimStart('/')}"

    private fun Map<String, String>.withoutContentType(): Map<String, String> =
        filterKeys { !it.equals("Content-Type", ignoreCase = true) }
}
