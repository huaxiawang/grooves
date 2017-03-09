package grooves.grails.mongo

import com.github.rahulsom.grooves.api.Snapshot

class PatientHealth implements Snapshot<Patient, String> {

    static mapWith = "mongo"

    String id
    Long lastEvent
    Set<String> processingErrors = []
    Long aggregateId
    @Override Patient getAggregate() { Patient.get(aggregateId) }
    @Override void setAggregate(Patient aggregate) { this.aggregateId = aggregate.id }
    Long deprecatedById
    @Override Patient getDeprecatedBy() { Patient.get(deprecatedById) }
    @Override void setDeprecatedBy(Patient aggregate) { deprecatedById = aggregate.id }
    Set<Long> deprecatesIds
    Set<Patient> getDeprecates() { deprecatesIds.collect {Patient.get(it)}.toSet() }
    void setDeprecates(Set<Patient> deprecates) { deprecatesIds = deprecates*.id }

    String name

    List<Procedure> procedures = []

    static hasMany = [
            procedures: Procedure,
            deprecatesIds: Long
    ]

    static constraints = {
        deprecatedById nullable: true
    }

    static embedded = ['procedures', 'processingErrors']
    static transients = ['aggregate', 'deprecatedBy', 'deprecates']
}

class Procedure {
    String code
    Date date
}