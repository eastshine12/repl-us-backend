package com.replus.api.mission.infrastructure.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("replus.storage")
data class StorageProperties(
    val mode: StorageMode = StorageMode.LOCAL,
    val local: LocalStorageProperties = LocalStorageProperties(),
)

enum class StorageMode {
    LOCAL,
    OBJECT_STORAGE,
}

data class LocalStorageProperties(
    val uploadBaseUrl: String = "http://localhost:8080/mock-upload",
    val playbackBaseUrl: String = "http://localhost:8080/mock-playback",
)
