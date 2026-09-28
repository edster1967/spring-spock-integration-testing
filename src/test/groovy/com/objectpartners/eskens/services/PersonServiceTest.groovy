package com.objectpartners.eskens.services

import com.objectpartners.eskens.entities.Person
import com.objectpartners.eskens.repos.PersonRepo
import spock.lang.Specification
import spock.lang.Subject

/**
 * A basic unit test w/o Spring components
 * Created by derek on 4/10/17.
 */
class PersonServiceTest extends Specification {

    PersonRepo personRepo = Mock()

    ExternalRankingService externalRankingService = Mock()

    @Subject PersonService personService = new PersonService(personRepo, externalRankingService)

    Person jamesKirk = new Person(id: 1L, firstName: 'James', lastName: 'Kirk', title: 'Capt')

    def "GetAddressToForPersonId"() {
        when:
        def addressTo = personService.getAddressToForPersonId(1L)

        then: 'Return an expected Person from our mocked repo'
        1 * personRepo.findById(1L) >> Optional.of(jamesKirk)

        and: 'AddressTo is formatted as expected'
        addressTo == 'Capt James Kirk'
    }

    def "GetRank by personId"() {
        when:
        def rank = personService.getRank(1L)

        then: 'Return an expected Rank from our mocked rank service'
        1 * personRepo.findById(1L) >> Optional.of(jamesKirk)
        1 * externalRankingService.getRank(jamesKirk) >> { new Rank(level: 1, classification: 'Captain') }

        and: 'we return the Rank from the external service unaltered'
        rank.level == 1
        rank.classification == 'Captain'
    }

    def "GetPerson throws PersonNotFoundException for an unknown id"() {
        when:
        personService.getPerson(42L)

        then:
        1 * personRepo.findById(42L) >> Optional.empty()
        def e = thrown(PersonNotFoundException)
        e.message == 'Person 42 not found'
    }

    def "GetRank does not call the external service for an unknown person"() {
        when:
        personService.getRank(42L)

        then:
        1 * personRepo.findById(42L) >> Optional.empty()
        0 * externalRankingService._
        thrown(PersonNotFoundException)
    }

    def "FindPersons returns everyone when no last name is given"() {
        when:
        def result = personService.findPersons(lastName)

        then:
        1 * personRepo.findAll() >> [jamesKirk]
        0 * personRepo.findByLastNameStartingWith(_)
        result == [jamesKirk]

        where:
        lastName << [null, '']
    }

    def "FindPersons filters by last name prefix"() {
        when:
        def result = personService.findPersons('Kir')

        then:
        1 * personRepo.findByLastNameStartingWith('Kir') >> [jamesKirk]
        0 * personRepo.findAll()
        result == [jamesKirk]
    }
}
