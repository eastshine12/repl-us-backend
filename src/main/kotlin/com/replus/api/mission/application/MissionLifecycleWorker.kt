package com.replus.api.mission.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class MissionLifecycleWorker(
    private val missionLifecycleFailureUseCase: MissionLifecycleFailureUseCase,
    private val clock: Clock,
    @Value("\${replus.mission.lifecycle.zone:Asia/Seoul}")
    val zoneIdValue: String,
) {
    fun failIncompleteMissionsBeforeToday(): MissionLifecycleFailureResult {
        val cutoffDate = LocalDate.now(clock.withZone(ZoneId.of(zoneIdValue)))
        return missionLifecycleFailureUseCase.failIncompleteMissionsBefore(cutoffDate)
    }
}
