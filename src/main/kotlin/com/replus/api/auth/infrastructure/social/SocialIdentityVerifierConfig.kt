package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.port.SocialIdentityVerifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class SocialIdentityVerifierConfig {
    @Bean
    @ConditionalOnMissingBean(SocialIdentityVerifier::class)
    fun unsupportedSocialIdentityVerifier(): SocialIdentityVerifier = UnsupportedSocialIdentityVerifier()
}
