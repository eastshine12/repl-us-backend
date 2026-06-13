package com.replus.api.auth.application

import com.replus.api.auth.application.port.SocialIdentityVerifier
import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProviderAccount
import com.replus.api.auth.domain.model.User
import com.replus.api.auth.domain.repository.AuthProviderAccountRepository
import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.common.security.SessionStore
import com.replus.api.mission.domain.repository.MissionRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.room.domain.repository.RoomMemberRepository
import com.replus.api.room.domain.repository.RoomRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class AuthFacade(
    private val userRepository: UserRepository,
    private val authProviderAccountRepository: AuthProviderAccountRepository,
    private val roomRepository: RoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val missionRepository: MissionRepository,
    private val missionResponseRepository: MissionResponseRepository,
    private val sessionStore: SessionStore,
    private val socialIdentityVerifier: SocialIdentityVerifier,
    private val clock: Clock,
    @Value("\${replus.auth.guest-session-enabled:true}")
    private val guestSessionEnabled: Boolean,
) {
    @Transactional
    fun createGuestSession(displayName: String?): AuthSessionResult {
        if (!guestSessionEnabled) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }

        val now = clock.instant()
        val user = userRepository.save(
            User(
                id = UUID.randomUUID(),
                displayName = displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Guest",
                avatarUrl = null,
                isGuest = true,
                createdAt = now,
            ),
        )
        return createSessionResult(user, now)
    }

    @Transactional
    fun loginWithSocialProvider(command: SocialLoginCommand): AuthSessionResult {
        val now = clock.instant()
        val identity = socialIdentityVerifier.verify(command)
        if (identity.provider != command.provider) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }
        val providerSubject = identity.providerSubject.trim().takeIf { it.isNotBlank() }
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)

        val account = authProviderAccountRepository.findByProviderAndProviderSubject(
            provider = identity.provider,
            providerSubject = providerSubject,
        )
        val user = if (account == null) {
            createSocialUser(identity, providerSubject, now)
        } else {
            authProviderAccountRepository.save(
                account.copy(
                    email = identity.email?.trim()?.takeIf { it.isNotBlank() },
                    displayName = identity.displayName.normalizedProviderDisplayName(),
                    avatarUrl = identity.avatarUrl?.trim()?.takeIf { it.isNotBlank() },
                    lastLoginAt = now,
                ),
            )
            userRepository.getById(account.userId)
        }

        return createSessionResult(user, now)
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(userId: UUID): CurrentUserResult {
        val user = userRepository.getById(userId)
        val today = today()
        val rooms = roomMemberRepository.findActiveByUserId(userId).map { member ->
            val room = roomRepository.getById(member.roomId)
            val latestMission = missionRepository.findLatestByRoomId(room.id)
            val todayMission = missionRepository.findByRoomIdAndMissionDate(room.id, today)
            val myTodayResponse = todayMission?.let {
                missionResponseRepository.findActiveByMissionIdAndMemberId(it.id, member.id)
            }
            RoomSummaryResult(
                room = room,
                memberCount = roomMemberRepository.countActiveByRoomId(room.id),
                currentMember = member,
                lastMissionDate = latestMission?.missionDate,
                today = todayMission?.let {
                    RoomTodaySummaryResult(
                        mission = it,
                        myResponseId = myTodayResponse?.id,
                    )
                },
            )
        }
        return CurrentUserResult(user = user, rooms = rooms)
    }

    private fun today(): LocalDate =
        LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))

    private fun createSocialUser(
        identity: VerifiedSocialIdentity,
        providerSubject: String,
        now: Instant,
    ): User {
        val user = userRepository.save(
            User(
                id = UUID.randomUUID(),
                displayName = identity.displayName.normalizedUserDisplayName(),
                avatarUrl = identity.avatarUrl?.trim()?.takeIf { it.isNotBlank() },
                isGuest = false,
                createdAt = now,
            ),
        )
        authProviderAccountRepository.save(
            AuthProviderAccount(
                id = UUID.randomUUID(),
                userId = user.id,
                provider = identity.provider,
                providerSubject = providerSubject,
                email = identity.email?.trim()?.takeIf { it.isNotBlank() },
                displayName = identity.displayName.normalizedProviderDisplayName(),
                avatarUrl = identity.avatarUrl?.trim()?.takeIf { it.isNotBlank() },
                linkedAt = now,
                lastLoginAt = now,
            ),
        )
        return user
    }

    private fun createSessionResult(user: User, now: Instant): AuthSessionResult {
        val expiresAt = now.plus(Duration.ofDays(30))
        return AuthSessionResult(
            accessToken = sessionStore.register(user.id, expiresAt),
            expiresAt = expiresAt,
            user = user,
        )
    }

    private fun String?.normalizedUserDisplayName(): String =
        normalizedProviderDisplayName()?.take(MAX_USER_DISPLAY_NAME_LENGTH) ?: DEFAULT_SOCIAL_DISPLAY_NAME

    private fun String?.normalizedProviderDisplayName(): String? =
        this?.trim()?.takeIf { it.isNotBlank() }?.take(MAX_PROVIDER_DISPLAY_NAME_LENGTH)

    private companion object {
        const val MAX_USER_DISPLAY_NAME_LENGTH = 24
        const val MAX_PROVIDER_DISPLAY_NAME_LENGTH = 100
        const val DEFAULT_SOCIAL_DISPLAY_NAME = "Member"
    }
}
