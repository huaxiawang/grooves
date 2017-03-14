package grooves.grails.rdbms

import com.github.rahulsom.grooves.api.Snapshot
import groovy.transform.ToString

@ToString
class PatientAccount implements Snapshot<Patient, Long> {

    Long lastEventPosition
    Date lastEventTimestamp
    Patient deprecatedBy
    Set<Patient> deprecates

    Long aggregateId
    Patient getAggregate() { Patient.get(aggregateId) }
    void setAggregate(Patient aggregate) { aggregateId = aggregate.id }

    BigDecimal balance = 0.0
    BigDecimal moneyMade = 0.0

    String name

    static hasMany = [
        deprecates: Patient
    ]

    static transients = ['aggregate']

    static constraints = {
        deprecatedBy nullable: true
    }
}