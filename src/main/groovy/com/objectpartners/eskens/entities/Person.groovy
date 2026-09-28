package com.objectpartners.eskens.entities

import groovy.transform.Canonical
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = 'person')
@Canonical
class Person {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    String firstName

    String lastName

    String title
}
