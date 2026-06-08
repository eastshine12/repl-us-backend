package com.replus.api.mission.infrastructure.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AwsS3PutObjectPresignerTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-05-24T09:20:00Z"), ZoneOffset.UTC)

    @Test
    fun `put object presign request contains object metadata and expiration duration`() {
        // given
        var recordedRequest: PutObjectPresignRequest? = null
        val sdkPresigner = fakeS3Presigner { _, args ->
            recordedRequest = args?.single() as PutObjectPresignRequest
            PresignedPutObjectRequest.builder()
                .expiration(Instant.parse("2026-05-24T09:25:00Z"))
                .isBrowserExecutable(false)
                .httpRequest(
                    SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.PUT)
                        .uri(URI.create("https://object-storage.example.dev/signed-upload"))
                        .build(),
                )
                .signedHeaders(
                    mapOf(
                        "content-type" to listOf("video/webm"),
                        "x-amz-checksum-crc32" to listOf("checksum"),
                    ),
                )
                .build()
        }
        val presigner = AwsS3PutObjectPresigner(sdkPresigner, fixedClock)

        // when
        val presigned = presigner.presignPutObject(
            S3PresignPutObjectRequest(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                contentType = "video/webm",
                expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
                maxFileSizeBytes = 15_000_000,
            ),
        )

        // then
        val putObjectRequest = requireNotNull(recordedRequest?.putObjectRequest())
        assertThat(putObjectRequest.bucket()).isEqualTo("replus-dev-videos")
        assertThat(putObjectRequest.key()).isEqualTo("rooms/room-id/missions/mission-id/members/member-id.webm")
        assertThat(putObjectRequest.contentType()).isEqualTo("video/webm")
        assertThat(putObjectRequest.getValueForField("ContentLength", Long::class.javaObjectType)).isEmpty
        assertThat(recordedRequest?.signatureDuration()).isEqualTo(Duration.ofMinutes(5))
        assertThat(presigned.uploadUrl).isEqualTo("https://object-storage.example.dev/signed-upload")
        assertThat(presigned.requiredHeaders).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "content-type" to "video/webm",
                "x-amz-checksum-crc32" to "checksum",
            ),
        )
    }

    private fun fakeS3Presigner(handler: (method: Method, args: Array<Any?>?) -> Any?): S3Presigner =
        Proxy.newProxyInstance(
            S3Presigner::class.java.classLoader,
            arrayOf(S3Presigner::class.java),
        ) { _, method, args ->
            when (method.name) {
                "close" -> null
                "serviceName" -> "s3"
                "toString" -> "FakeS3Presigner"
                else -> handler(method, args)
            }
        } as S3Presigner
}
