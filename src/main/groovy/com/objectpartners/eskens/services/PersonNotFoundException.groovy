package com.objectpartners.eskens.services

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class PersonNotFoundException extends RuntimeException {

    PersonNotFoundException(Long personId) {
        super("Person $personId not found".toString())
    }
}
