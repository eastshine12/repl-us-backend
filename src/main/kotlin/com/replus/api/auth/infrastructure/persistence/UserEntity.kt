package com.replus.api.auth.infrastructure.persistence

import com.replus.api.auth.domain.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "display_name", nullable = false, length = 24)
    var displayName: String,

    @Column(name = "avatar_url")
    var avatarUrl: String?,

    @Column(name = "is_guest", nullable = false)
    var isGuest: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): User =
        User(
            id = id,
            displayName = displayName,
            avatarUrl = avatarUrl,
            isGuest = isGuest,
            createdAt = createdAt,
        )

    companion object {
        fun from(user: User): UserEntity =
            UserEntity(
                id = user.id,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                isGuest = user.isGuest,
                createdAt = user.createdAt,
            )
    }
}
