package com.replus.api.common.config

import com.replus.api.mission.domain.policy.MissionEditPolicy
import com.replus.api.room.domain.policy.RoomAccessPolicy
import com.replus.api.room.domain.policy.RoomCapacityPolicy
import com.replus.api.room.domain.service.InviteCodeGenerator
import com.replus.api.room.domain.service.OpaqueInviteCodeGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainPolicyConfig {
    @Bean
    fun roomAccessPolicy(): RoomAccessPolicy = RoomAccessPolicy()

    @Bean
    fun roomCapacityPolicy(): RoomCapacityPolicy = RoomCapacityPolicy()

    @Bean
    fun inviteCodeGenerator(): InviteCodeGenerator = OpaqueInviteCodeGenerator()

    @Bean
    fun missionEditPolicy(): MissionEditPolicy = MissionEditPolicy()
}
