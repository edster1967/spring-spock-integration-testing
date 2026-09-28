package com.objectpartners.eskens.services

import com.objectpartners.eskens.entities.Person
import com.objectpartners.eskens.repos.PersonRepo
import org.springframework.stereotype.Service

@Service
class PersonService {

    final PersonRepo personRepo

    final ExternalRankingService externalRankingService

    PersonService(PersonRepo pr, ExternalRankingService ers) {
        this.personRepo = pr
        this.externalRankingService = ers
    }

    List<Person> findPersons(String lastName) {
        lastName ? personRepo.findByLastNameStartingWith(lastName) : personRepo.findAll()
    }

    Person getPerson(Long personId) {
        personRepo.findById(personId).orElseThrow { new PersonNotFoundException(personId) }
    }

    String getAddressToForPersonId(Long personId) {
        def p = getPerson(personId)
        "$p.title $p.firstName $p.lastName"
    }

    Rank getRank(Long personId) {
        externalRankingService.getRank(getPerson(personId))
    }
}
