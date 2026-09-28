package com.objectpartners.eskens.config

import com.objectpartners.eskens.services.ExternalRankingService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import spock.mock.DetachedMockFactory

/**
 * This is the key.  The DetachedMockFactory allows us to create Mocks outside the Spec.
 * That, combined with TestConfiguration, means we can define new beans here using Mock objects.
 * We can then inject these mocks into the spec.
 * (Spring Boot 2.1+ disables bean overriding by default, so the mock is registered as the @Primary bean.)
 */
@TestConfiguration
class IntegrationTestMockingConfig {
    private DetachedMockFactory factory = new DetachedMockFactory()

    @Bean
    @Primary
    ExternalRankingService externalRankingServiceMock() {
        factory.Mock(ExternalRankingService)
    }
}
