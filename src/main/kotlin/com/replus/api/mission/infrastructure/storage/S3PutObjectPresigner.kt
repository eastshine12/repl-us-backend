package com.replus.api.mission.infrastructure.storage

import java.time.Instant

interface S3PutObjectPresigner {
    fun presignPutObject(request: S3PresignPutObjectRequest): S3PresignedPutObject
}

data class S3PresignPutObjectRequest(
    val bucket: String,
    val objectKey: String,
    val contentType: String,
    val expiresAt: Instant,
    val maxFileSizeBytes: Long,
)

data class S3PresignedPutObject(
    val uploadUrl: String,
    val requiredHeaders: Map<String, String>,
)
