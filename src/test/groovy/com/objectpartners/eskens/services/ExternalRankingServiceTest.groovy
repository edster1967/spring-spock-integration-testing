package com.objectpartners.eskens.services

import com.objectpartners.eskens.entities.Person
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import spock.lang.Specification

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 * Unit tests for the external ranking client, with the HTTP server stubbed by MockRestServiceServer
 */
class ExternalRankingServiceTest extends Specification {

    Person jamesKirk = new Person(id: 1L, firstName: 'James', lastName: 'Kirk', title: 'Capt')

    def "returns Unranked without making an HTTP call when no API url is configured"() {
        given:
        def builder = RestClient.builder()
        def server = MockRestServiceServer.bindTo(builder).build()
        def service = new ExternalRankingService(builder, '', 'secret-key')

        expect:
        service.getRank(jamesKirk) == new Rank(level: 0, classification: 'Unranked')
        server.verify()
    }

    def "calls the external API with the configured API key and maps the response"() {
        given:
        def builder = RestClient.builder()
        def server = MockRestServiceServer.bindTo(builder).build()
        def service = new ExternalRankingService(builder, 'https://ranking.example.com', 'secret-key')

        server.expect(requestTo('https://ranking.example.com/ranks/1'))
                .andExpect(method(GET))
                .andExpect(header('X-API-KEY', 'secret-key'))
                .andRespond(withSuccess('{"level":1,"classification":"Captain"}', APPLICATION_JSON))

        when:
        def rank = service.getRank(jamesKirk)

        then:
        rank == new Rank(level: 1, classification: 'Captain')
        server.verify()
    }

    def "propagates errors from the external API"() {
        given:
        def builder = RestClient.builder()
        def server = MockRestServiceServer.bindTo(builder).build()
        def service = new ExternalRankingService(builder, 'https://ranking.example.com', 'secret-key')
        server.expect(requestTo('https://ranking.example.com/ranks/1')).andRespond(withServerError())

        when:
        service.getRank(jamesKirk)

        then:
        thrown(HttpServerErrorException)
    }
}
