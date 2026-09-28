package com.objectpartners.eskens.controllers

import com.objectpartners.eskens.config.IntegrationTestMockingConfig
import com.objectpartners.eskens.services.ExternalRankingService
import com.objectpartners.eskens.services.Rank
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * An integration test illustrating how to wire everything w/ Spring,
 * but replace certain components with Spock mocks
 * Created by derek on 4/10/17.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
@Import([IntegrationTestMockingConfig]) //See additional notes at the bottom
class PersonControllerIntTest extends Specification {

    @Autowired MockMvc mvc

    /**
     * This is our mock we created in our test config. We inject it in so we can control it in our specs.
     */
    @Autowired ExternalRankingService externalRankingServiceMock

    def "GetRank"() {
        when: 'Calling getRank for a known seed data entity'
        MvcResult mvcResult = mvc.perform(get("/persons/1/rank").contentType(APPLICATION_JSON))
                                .andExpect(status().is2xxSuccessful()).andReturn()

        then: 'we define the mock for JUST the external service'
        1 * externalRankingServiceMock.getRank({ it.lastName == 'Kirk' }) >> {
            new Rank(level: 1, classification: 'Captain')
        }
        noExceptionThrown()

        when: 'inspecting the contents'
        def resultingJson = mvcResult.response.contentAsString

        then: 'the result contains a mix of mocked service data and actual wired component data'
        resultingJson == 'Capt James Kirk ~ Captain:Level 1'
    }

    def "GetRank returns 404 for an unknown person without calling the external service"() {
        when:
        def result = mvc.perform(get('/persons/999/rank'))

        then:
        result.andExpect(status().isNotFound())
        0 * externalRankingServiceMock.getRank(_)
    }

    def "GetPersons lists all seeded persons"() {
        expect:
        mvc.perform(get('/persons').accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.length()').value(1))
                .andExpect(jsonPath('$[0].id').value(1))
                .andExpect(jsonPath('$[0].firstName').value('James'))
                .andExpect(jsonPath('$[0].lastName').value('Kirk'))
                .andExpect(jsonPath('$[0].title').value('Capt'))
    }

    def "GetPersons filters by last name prefix '#lastName'"() {
        expect:
        mvc.perform(get('/persons').param('lastName', lastName).accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.length()').value(expectedCount))

        where:
        lastName | expectedCount
        'Kir'    | 1
        'Kirk'   | 1
        'Spo'    | 0
    }

    def "GetPerson by id"() {
        expect:
        mvc.perform(get('/persons/1').accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.firstName').value('James'))
                .andExpect(jsonPath('$.lastName').value('Kirk'))
    }

    def "GetPerson returns 404 for an unknown id"() {
        expect:
        mvc.perform(get('/persons/999').accept(APPLICATION_JSON))
                .andExpect(status().isNotFound())
    }

    def "GetPerson returns 400 for a non-numeric id"() {
        expect:
        mvc.perform(get('/persons/abc').accept(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
    }

    /*
        We could define our test configuration here, but if we have multiple integration tests
        and we want to mock the same things, then it's better to share the configuration for context caching,
        thus the import of IntegrationTestMockingConfig
     */

    /*
    @TestConfiguration
    static class Config {
        private DetachedMockFactory factory = new DetachedMockFactory()

        @Bean
        ExternalRankingService externalRankingService() {
            factory.Mock(ExternalRankingService)
        }
    }
    */
}
