package com.replus.api.mission.infrastructure.storage

interface S3HeadObjectClient {
    fun headObject(request: S3HeadObjectRequest): S3HeadObjectResult
}

data class S3HeadObjectRequest(
    val bucket: String,
    val objectKey: String,
)

data class S3HeadObjectResult(
    val contentType: String?,
    val fileSizeBytes: Long?,
)

class S3ObjectNotFoundException(
    cause: Throwable? = null,
) : RuntimeException("Object storage object was not found", cause)
