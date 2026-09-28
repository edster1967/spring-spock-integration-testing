package com.objectpartners.eskens.db

import com.objectpartners.eskens.repos.PersonRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import spock.lang.Requires
import spock.lang.Specification

/**
 * Runs the app against a real PostgreSQL database in Docker using the "postgres" profile.
 * Skipped automatically when Docker is not available.
 */
@Requires({ DockerClientFactory.instance().isDockerAvailable() })
@SpringBootTest
@ActiveProfiles('postgres')
class PostgresProfileIntTest extends Specification {

    private static PostgreSQLContainer postgres

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        if (postgres == null) {
            postgres = new PostgreSQLContainer('postgres:17')
            postgres.start()
        }
        registry.add('POSTGRES_URL') { postgres.jdbcUrl }
        registry.add('POSTGRES_USER') { postgres.username }
        registry.add('POSTGRES_PASSWORD') { postgres.password }
        registry.add('ranking.api.url') { '' }
    }

    @Autowired PersonRepo personRepo

    void 'schema.sql and data.sql work on PostgreSQL'() {
        expect:
        personRepo.findByLastNameStartingWith('Kir')*.firstName == ['James']
    }
}
