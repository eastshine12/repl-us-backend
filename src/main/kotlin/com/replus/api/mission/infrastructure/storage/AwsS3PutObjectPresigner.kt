package com.replus.api.mission.infrastructure.storage

import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Clock
import java.time.Duration

class AwsS3PutObjectPresigner(
    private val s3Presigner: S3Presigner,
    private val clock: Clock,
) : S3PutObjectPresigner {
    override fun presignPutObject(request: S3PresignPutObjectRequest): S3PresignedPutObject {
        val presigned = s3Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.between(clock.instant(), request.expiresAt))
                .putObjectRequest(
                    PutObjectRequest.builder()
                        .bucket(request.bucket)
                        .key(request.objectKey)
                        .contentType(request.contentType)
                        .build(),
                )
                .build(),
        )

        return S3PresignedPutObject(
            uploadUrl = presigned.url().toString(),
            requiredHeaders = presigned.signedHeaders().mapValues { (_, values) -> values.joinToString(",") },
        )
    }
}
