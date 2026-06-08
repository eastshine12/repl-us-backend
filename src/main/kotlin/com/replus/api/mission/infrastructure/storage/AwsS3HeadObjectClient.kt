package com.replus.api.mission.infrastructure.storage

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception

class AwsS3HeadObjectClient(
    private val s3Client: S3Client,
) : S3HeadObjectClient {
    override fun headObject(request: S3HeadObjectRequest): S3HeadObjectResult =
        try {
            val response = s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(request.bucket)
                    .key(request.objectKey)
                    .build(),
            )
            S3HeadObjectResult(
                contentType = response.contentType(),
                fileSizeBytes = response.contentLength(),
            )
        } catch (exception: NoSuchKeyException) {
            throw S3ObjectNotFoundException(exception)
        } catch (exception: S3Exception) {
            if (exception.statusCode() == 404) {
                throw S3ObjectNotFoundException(exception)
            }
            throw exception
        }
}
