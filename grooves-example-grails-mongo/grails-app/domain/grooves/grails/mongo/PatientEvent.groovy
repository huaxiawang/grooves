package grooves.grails.mongo


import com.github.rahulsom.grooves.annotations.Event
import com.github.rahulsom.grooves.api.internal.BaseEvent
import com.github.rahulsom.grooves.api.RevertEvent
import groovy.json.JsonBuilder

abstract class PatientEvent implements BaseEvent<Patient, PatientEvent> {

    RevertEvent<Patient, PatientEvent> revertedBy
    String createdBy
    Date timestamp
    Long position
    Patient aggregate

    static transients = ['revertedBy']

    static constraints = {
    }
}

@Event(Patient)
class PatientCreated extends PatientEvent {
    String name

    @Override String getAudit() { new JsonBuilder([name: name]).toString() }
}

@Event(Patient)
class ProcedurePerformed extends PatientEvent {
    String code
    BigDecimal cost

    @Override String getAudit() { new JsonBuilder([code: code, cost: cost]).toString() }
}

@Event(Patient)
class PaymentMade extends PatientEvent {
    BigDecimal amount

    @Override String getAudit() { new JsonBuilder([amount: amount]).toString() }
}

class PatientEventReverted extends PatientEvent implements RevertEvent<Patient, PatientEvent> {
    Long revertedEventId

    @Override String getAudit() { new JsonBuilder([revertedEvent: revertedEventId]).toString() }
}