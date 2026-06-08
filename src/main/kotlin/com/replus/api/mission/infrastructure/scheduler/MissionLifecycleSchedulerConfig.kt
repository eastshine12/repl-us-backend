package com.replus.api.mission.infrastructure.scheduler

import com.replus.api.mission.application.MissionLifecycleWorker
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
class MissionLifecycleSchedulerConfig {
    @Bean
    @ConditionalOnProperty(
        prefix = "replus.mission.lifecycle.scheduler",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    fun missionLifecycleScheduler(worker: MissionLifecycleWorker): MissionLifecycleScheduler =
        MissionLifecycleScheduler(worker)
}

class MissionLifecycleScheduler(
    private val worker: MissionLifecycleWorker,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        cron = "\${replus.mission.lifecycle.scheduler.fail-incomplete-cron:0 5 0 * * *}",
        zone = "\${replus.mission.lifecycle.zone:Asia/Seoul}",
    )
    fun failIncompleteMissionsBeforeToday() {
        val result = worker.failIncompleteMissionsBeforeToday()
        log.info("mission_lifecycle_failed_incomplete count={}", result.failedMissionIds.size)
    }

    @Scheduled(
        fixedDelayString = "\${replus.mission.lifecycle.scheduler.release-due-fixed-delay:30000}",
        initialDelayString = "\${replus.mission.lifecycle.scheduler.release-due-initial-delay:30000}",
    )
    fun releaseDueMissions() {
        val result = worker.releaseDueMissions()
        log.info("mission_lifecycle_released_due count={}", result.releasedMissionIds.size)
    }
}
