package com.github.rahulsom.grooves.test

import groovyx.net.http.RESTClient
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Base test to replicate across example projects
 *
 * @author Rahul Somasunderam
 */
abstract class AbstractPatientSpec extends Specification {

    abstract RESTClient getRest()

    void "Patient List works"() {
        when:
        def resp = rest.get(path: '/patient.json')

        then:
        with(resp) {
            status == 200
            contentType == "application/json"
        }
        with(resp.data) {
            it[0].uniqueId == '42'
            it[1].uniqueId == '43'
        }
    }

    @Unroll
    void "Paul McCartney's balance is correct at version #version"() {
        given:
        def resp = rest.get(path: "/patient/account/3.json".toString(), params: [version: version])

        expect:
        with(resp) {
            status == 200
            contentType == "application/json"
        }
        with(resp.data) {
            it.balance == balance
            it.moneyMade == moneyMade
        }

        where:
        version | balance | moneyMade
        1       | 0.0     | 0.0
        2       | 170.0   | 0.0
        3       | 248.93  | 0.0
        4       | 148.68  | 100.25
        5       | 69.75   | 100.25
        6       | 39.75   | 130.25
        7       | 69.75   | 100.25
        8       | 9.75    | 160.25
        9       | -50.25  | 220.25
    }

    @Unroll
    void "#name - Show works"() {
        given:
        def resp = rest.get(path: "/patient/show/${id}.json".toString())

        expect:
        with(resp) {
            status == 200
            contentType == "application/json"
        }
        with(resp.data) {
            it.id == id
            it.uniqueId == uniqueId
        }

        where:
        id | name          || uniqueId
        1  | 'John Lennon' || '42'
        2  | 'Ringo Starr' || '43'
    }

    @Unroll
    void "#name - Health works"() {
        given:
        def resp = rest.get(path: "/patient/health/${id}.json".toString())
        println resp.data

        expect:
        with(resp) {
            status == 200
            contentType == "application/json"
        }
        with(resp.data) {
            it.aggregateId == id || it.aggregate.id == id
            it.name == name
            it.lastEventPosition == lastEventPosition
            it.procedures.size() == codes.size()
            it.procedures*.code == codes
        }

        where:
        id | name             || lastEventPosition | codes
        1  | 'John Lennon'    || 6                 | ['FLUSHOT', 'GLUCOSETEST', 'ANNUALPHYSICAL']
        2  | 'Ringo Starr'    || 6                 | ['ANNUALPHYSICAL', 'GLUCOSETEST', 'FLUSHOT']
        3  | 'Paul McCartney' || 9                 | ['ANNUALPHYSICAL']
    }

    @Unroll
    void "#name by Version #version - Health works"() {
        given:
        def resp = rest.get(path: "/patient/health/${id}.json".toString(), query: [version: version])
        println resp.data

        expect:
        with(resp) {
            status == 200
            contentType == "application/json"
        }
        with(resp.data) {
            it.aggregateId == id || it.aggregate.id == id
            it.name == name
            it.lastEventPosition == version
            it.procedures.size() == codes.size()
            it.procedures*.code == codes
        }

        where:
        id | version || name             | codes
        1  | 1       || 'John Lennon'    | []
        1  | 2       || 'John Lennon'    | ['FLUSHOT']
        1  | 3       || 'John Lennon'    | ['FLUSHOT', 'GLUCOSETEST']
        1  | 5       || 'John Lennon'    | ['FLUSHOT', 'GLUCOSETEST', 'ANNUALPHYSICAL']
        2  | 1       || 'Ringo Starr'    | []
        2  | 2       || 'Ringo Starr'    | ['ANNUALPHYSICAL']
        2  | 3       || 'Ringo Starr'    | ['ANNUALPHYSICAL', 'GLUCOSETEST']
        2  | 5       || 'Ringo Starr'    | ['ANNUALPHYSICAL', 'GLUCOSETEST', 'FLUSHOT']
        3  | 1       || 'Paul McCartney' | []
        3  | 2       || 'Paul McCartney' | ['ANNUALPHYSICAL']
        3  | 3       || 'Paul McCartney' | ['ANNUALPHYSICAL', 'GLUCOSETEST']
        3  | 5       || 'Paul McCartney' | ['ANNUALPHYSICAL']
    }

    @Unroll
    def "#name by Date #date - Health works"() {
        given:
        def resp = rest.get(path: "/patient/health/${id}.json".toString(), query: [date: date])
        println resp.data

        expect:
        with(resp) {
            status == 200
            contentType == "application/json"
        }
        with(resp.data) {
            it.aggregateId == id || it.aggregate.id == id
            it.name == name
            it.lastEventPosition == lastEventPosition
            it.procedures.size() == codes.size()
            it.procedures*.code == codes
        }

        where:
        id | date         || lastEventPosition | name             | codes
        1  | '2016-01-03' || 2                 | 'John Lennon'    | ['FLUSHOT']
        2  | '2016-01-09' || 2                 | 'Ringo Starr'    | ['ANNUALPHYSICAL']
        3  | '2016-01-15' || 2                 | 'Paul McCartney' | ['ANNUALPHYSICAL']
        3  | '2016-01-16' || 3                 | 'Paul McCartney' | ['ANNUALPHYSICAL', 'GLUCOSETEST']
        3  | '2016-01-18' || 5                 | 'Paul McCartney' | ['ANNUALPHYSICAL']
    }
}
