package com.replus.api.mission.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
open class MissionLifecycleWorker(
    private val missionLifecycleFailureUseCase: MissionLifecycleFailureUseCase,
    private val missionLifecycleReleaseUseCase: MissionLifecycleReleaseUseCase,
    private val clock: Clock,
    @Value("\${replus.mission.lifecycle.zone:Asia/Seoul}")
    val zoneIdValue: String,
) {
    fun failIncompleteMissionsBeforeToday(): MissionLifecycleFailureResult {
        val cutoffDate = LocalDate.now(clock.withZone(ZoneId.of(zoneIdValue)))
        return missionLifecycleFailureUseCase.failIncompleteMissionsBefore(cutoffDate)
    }

    open fun releaseDueMissions(): MissionLifecycleReleaseResult {
        val dueAt: Instant = clock.instant()
        return missionLifecycleReleaseUseCase.releaseDueMissions(dueAt)
    }
}
