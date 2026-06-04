package com.replus.api.mission.infrastructure.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Clock

@Configuration
@ConditionalOnProperty(prefix = "replus.storage", name = ["mode"], havingValue = "object-storage")
@EnableConfigurationProperties(StorageProperties::class)
class ObjectStorageClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun s3Client(properties: StorageProperties): S3Client {
        val objectStorage = properties.objectStorage
        val builder = S3Client.builder()
            .region(Region.of(objectStorage.region.trim()))
            .serviceConfiguration(s3Configuration(objectStorage))

        objectStorage.endpoint.trim()
            .takeIf { it.isNotBlank() }
            ?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun s3Presigner(properties: StorageProperties): S3Presigner {
        val objectStorage = properties.objectStorage
        val builder = S3Presigner.builder()
            .region(Region.of(objectStorage.region.trim()))
            .serviceConfiguration(s3Configuration(objectStorage))

        objectStorage.endpoint.trim()
            .takeIf { it.isNotBlank() }
            ?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun s3PutObjectPresigner(
        s3Presigner: S3Presigner,
        clock: Clock,
    ): S3PutObjectPresigner =
        AwsS3PutObjectPresigner(s3Presigner, clock)

    @Bean
    @ConditionalOnMissingBean(ObjectStorageUploadSigner::class)
    fun s3ObjectStorageUploadSigner(putObjectPresigner: S3PutObjectPresigner): ObjectStorageUploadSigner =
        S3ObjectStorageUploadSigner(putObjectPresigner)

    @Bean
    @ConditionalOnMissingBean
    fun s3HeadObjectClient(s3Client: S3Client): S3HeadObjectClient =
        AwsS3HeadObjectClient(s3Client)

    @Bean
    @ConditionalOnMissingBean(ObjectStorageUploadVerifier::class)
    fun s3ObjectStorageUploadVerifier(headObjectClient: S3HeadObjectClient): ObjectStorageUploadVerifier =
        S3ObjectStorageUploadVerifier(headObjectClient)

    private fun s3Configuration(properties: ObjectStorageProperties): S3Configuration =
        S3Configuration.builder()
            .pathStyleAccessEnabled(properties.pathStyleAccessEnabled)
            .build()
}
