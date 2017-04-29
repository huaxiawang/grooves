package grooves.grails.mongo

import com.github.rahulsom.grooves.api.snapshots.Join
import groovy.transform.EqualsAndHashCode
import rx.Observable

import static rx.Observable.*

/**
 * Joins Doctor with Patients
 *
 * @author Rahul Somasunderam
 */
@EqualsAndHashCode(includes = ['aggregateId', 'lastEventPosition',])
class DoctorPatients implements Join<Doctor, String, Long, Long, DoctorEvent> {

    static mapWith = 'mongo'

    String id
    Long lastEventPosition
    Date lastEventTimestamp
    Set<String> processingErrors = []

    Long aggregateId

    Doctor getAggregate() { Doctor.get(aggregateId) }

    @Override
    Observable<Doctor> getAggregateObservable() {
        aggregateId ? just(aggregate) : empty()
    }

    void setAggregate(Doctor aggregate) { this.aggregateId = aggregate.id }

    @Override
    Observable<Doctor> getDeprecatedByObservable() {
        deprecatedBy ? just(deprecatedBy) : empty()
    }
    Long deprecatedById

    Doctor getDeprecatedBy() { Doctor.get(deprecatedById) }

    void setDeprecatedBy(Doctor aggregate) { deprecatedById = aggregate.id }

    @Override
    Observable<Doctor> getDeprecatesObservable() {
        deprecatesIds ? from(deprecatesIds).flatMap { Doctor.get(it) } : empty()
    }
    Set<Long> deprecatesIds

    Set<Doctor> getDeprecates() { deprecatesIds.collect { Doctor.get(it) }.toSet() }

    void setDeprecates(Set<Doctor> deprecates) { deprecatesIds = deprecates*.id }

    List<Long> joinedIds

    static hasMany = [
            deprecatesIds: Long,
    ]

    static constraints = {
        deprecatedById nullable: true
    }

    static embedded = ['procedures', 'processingErrors',]
    static transients = ['aggregate', 'deprecatedBy', 'deprecates',]

    @Override String toString() { "DoctorPatients($id, $aggregateId, $lastEventPosition)" }
}
