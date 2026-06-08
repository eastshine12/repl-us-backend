package com.replus.api.mission.infrastructure.storage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class AwsS3HeadObjectClientTest {
    @Test
    fun `head object response is mapped to storage head result`() {
        // given
        var recordedRequest: HeadObjectRequest? = null
        val sdkClient = fakeS3Client { _, args ->
            recordedRequest = args?.single() as HeadObjectRequest
            HeadObjectResponse.builder()
                .contentType("video/webm")
                .contentLength(842120)
                .build()
        }
        val client = AwsS3HeadObjectClient(sdkClient)

        // when
        val result = client.headObject(
            S3HeadObjectRequest(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
            ),
        )

        // then
        assertThat(recordedRequest?.bucket()).isEqualTo("replus-dev-videos")
        assertThat(recordedRequest?.key()).isEqualTo("rooms/room-id/missions/mission-id/members/member-id.webm")
        assertThat(result.contentType).isEqualTo("video/webm")
        assertThat(result.fileSizeBytes).isEqualTo(842120)
    }

    @Test
    fun `s3 404 is mapped to object not found exception`() {
        // given
        val failure = S3Exception.builder()
            .statusCode(404)
            .message("not found")
            .build()
        val client = AwsS3HeadObjectClient(fakeS3Client { _, _ -> throw failure })

        // expect
        assertThatThrownBy {
            client.headObject(
                S3HeadObjectRequest(
                    bucket = "replus-dev-videos",
                    objectKey = "missing.webm",
                ),
            )
        }
            .isInstanceOf(S3ObjectNotFoundException::class.java)
            .hasCause(failure)
    }

    @Test
    fun `non 404 s3 failure is propagated`() {
        // given
        val failure = S3Exception.builder()
            .statusCode(500)
            .message("storage unavailable")
            .build()
        val client = AwsS3HeadObjectClient(fakeS3Client { _, _ -> throw failure })

        // expect
        assertThatThrownBy {
            client.headObject(
                S3HeadObjectRequest(
                    bucket = "replus-dev-videos",
                    objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                ),
            )
        }.isSameAs(failure)
    }

    private fun fakeS3Client(handler: (method: Method, args: Array<Any?>?) -> Any?): S3Client =
        Proxy.newProxyInstance(
            S3Client::class.java.classLoader,
            arrayOf(S3Client::class.java),
        ) { _, method, args ->
            when (method.name) {
                "close" -> null
                "serviceName" -> "s3"
                "toString" -> "FakeS3Client"
                else -> handler(method, args)
            }
        } as S3Client
}
