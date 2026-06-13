package com.replus.api.auth.application

import com.replus.api.auth.domain.model.User
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class AuthFacade(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val missionRepository: MissionRepository,
    private val missionResponseRepository: MissionResponseRepository,
    private val sessionStore: SessionStore,
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
        val expiresAt = now.plus(Duration.ofDays(30))
        return AuthSessionResult(
            accessToken = sessionStore.register(user.id, expiresAt),
            expiresAt = expiresAt,
            user = user,
        )
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
}
