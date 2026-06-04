package com.replus.api.mission.infrastructure.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

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
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(objectStorage.pathStyleAccessEnabled)
                    .build(),
            )

        objectStorage.endpoint.trim()
            .takeIf { it.isNotBlank() }
            ?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun s3HeadObjectClient(s3Client: S3Client): S3HeadObjectClient =
        AwsS3HeadObjectClient(s3Client)

    @Bean
    @ConditionalOnMissingBean(ObjectStorageUploadVerifier::class)
    fun objectStorageUploadVerifier(headObjectClient: S3HeadObjectClient): ObjectStorageUploadVerifier =
        S3ObjectStorageUploadVerifier(headObjectClient)
}
