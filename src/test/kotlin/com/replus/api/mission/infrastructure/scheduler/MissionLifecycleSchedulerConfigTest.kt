package com.replus.api.mission.infrastructure.scheduler

import com.replus.api.mission.application.MissionLifecycleFailureResult
import com.replus.api.mission.application.MissionLifecycleReleaseResult
import com.replus.api.mission.application.MissionLifecycleWorker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

class MissionLifecycleSchedulerConfigTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(MissionLifecycleSchedulerConfig::class.java, WorkerConfig::class.java)

    @Test
    fun `scheduler는 기본 설정에서 등록되지 않는다`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(MissionLifecycleScheduler::class.java)
        }
    }

    @Test
    fun `scheduler는 enabled 설정에서 등록된다`() {
        contextRunner
            .withPropertyValues("replus.mission.lifecycle.scheduler.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(MissionLifecycleScheduler::class.java)
            }
    }

    @Test
    fun `scheduler는 release due worker를 호출할 수 있다`() {
        contextRunner
            .withPropertyValues("replus.mission.lifecycle.scheduler.enabled=true")
            .run { context ->
                val scheduler = context.getBean(MissionLifecycleScheduler::class.java)
                val worker = context.getBean(RecordingMissionLifecycleWorker::class.java)

                scheduler.releaseDueMissions()

                assertThat(worker.releaseDueCallCount).isEqualTo(1)
            }
    }

    @Configuration
    private class WorkerConfig {
        @Bean
        fun missionLifecycleWorker(): MissionLifecycleWorker =
            RecordingMissionLifecycleWorker()
    }

    private class RecordingMissionLifecycleWorker : MissionLifecycleWorker(
        missionLifecycleFailureUseCase = { _ -> MissionLifecycleFailureResult(failedMissionIds = emptyList()) },
        missionLifecycleReleaseUseCase = { _ -> MissionLifecycleReleaseResult(releasedMissionIds = emptyList()) },
        clock = Clock.systemUTC(),
        zoneIdValue = "Asia/Seoul",
    ) {
        var releaseDueCallCount = 0

        override fun releaseDueMissions(): MissionLifecycleReleaseResult {
            releaseDueCallCount += 1
            return MissionLifecycleReleaseResult(releasedMissionIds = emptyList())
        }
    }
}
