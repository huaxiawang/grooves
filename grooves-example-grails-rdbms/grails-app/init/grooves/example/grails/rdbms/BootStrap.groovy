package grooves.example.grails.rdbms

import com.github.rahulsom.grooves.api.EventsDsl
import grooves.grails.rdbms.*

import java.util.function.Consumer

class BootStrap {

    def init = { servletContext ->
        createJohnLennon()
        createRingoStarr()
        createPaulMcCartney()
        createGeorgeHarrison()
    }

    private Patient createJohnLennon() {
        def patient = new Patient(uniqueId: 'JL001').save(flush: true, failOnError: true)

        on(patient) {
            apply new PatientCreated(name: 'John Lennon')
            apply new ProcedurePerformed(code: 'FLUSHOT', cost: 32.40)
            apply new ProcedurePerformed(code: 'GLUCOSETEST', cost: 78.93)
            apply new PaymentMade(amount: 100.25)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()

            apply new ProcedurePerformed(code: 'ANNUALPHYSICAL', cost: 170.00)
            apply new PaymentMade(amount: 180.00)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()
        }
    }

    private Patient createRingoStarr() {
        def patient = new Patient(uniqueId: 'RS042').save(flush: true, failOnError: true)

        on(patient) {
            apply new PatientCreated(name: 'Ringo Starr')
            apply new ProcedurePerformed(code: 'ANNUALPHYSICAL', cost: 170.00)
            apply new ProcedurePerformed(code: 'GLUCOSETEST', cost: 78.93)
            apply new PaymentMade(amount: 100.25)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()

            apply new ProcedurePerformed(code: 'FLUSHOT', cost: 32.40)
            apply new PaymentMade(amount: 180.00)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()
        }
    }

    private Patient createPaulMcCartney() {
        def patient = new Patient(uniqueId: 'PMC02').save(flush: true, failOnError: true)

        on(patient) {
            apply new PatientCreated(name: 'Paul McCartney')
            apply new ProcedurePerformed(code: 'ANNUALPHYSICAL', cost: 170.00)
            def gluc = apply new ProcedurePerformed(code: 'GLUCOSETEST', cost: 78.93)
            apply new PaymentMade(amount: 100.25)
            apply new PatientEventReverted(revertedEventId: gluc.id)
            def pmt = apply new PaymentMade(amount: 30.00)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()

            apply new PatientEventReverted(revertedEventId: pmt.id)
            apply new PaymentMade(amount: 60.00)


            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()

            apply new PaymentMade(amount: 60.00)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()
        }

    }

    private Patient createGeorgeHarrison() {
        def patient = new Patient(uniqueId: 'GH009').save(flush: true, failOnError: true)
        def patient2 = new Patient(uniqueId: 'GH017').save(flush: true, failOnError: true)

        on(patient) {
            apply new PatientCreated(name: 'George Harrison')
            apply new ProcedurePerformed(code: 'ANNUALPHYSICAL', cost: 170.00)
            apply new ProcedurePerformed(code: 'GLUCOSETEST', cost: 78.93)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()
        }

        on(patient2) {
            apply new PatientCreated(name: 'George Harrison, Member of the Most Excellent Order of the British Empire')
            apply new PaymentMade(amount: 100.25)

            snapshotWith new PatientAccountQuery()
            snapshotWith new PatientHealthQuery()
        }

        currDate += 1;
        merge(patient, patient2)
        patient
    }

    /**
     *
     * @param self The aggregate to be deprecated
     * @param into The aggregate to survive
     * @return
     */
    private PatientDeprecatedBy merge(Patient self, Patient into) {
        def e1 = new PatientDeprecatedBy(aggregate: self, createdBy: 'anonymous' , deprecator: into,
                timestamp: currDate, position: PatientEvent.countByAggregate(self) + 1)
        def e2 = new PatientDeprecates(aggregate: into, createdBy: 'anonymous' , deprecated: self,
                timestamp: currDate, converse: e1, position: PatientEvent.countByAggregate(into) + 1)
        e1.converse = e2
        e2.save(flush: true, failOnError: true)
        e2.converse
    }

    Date currDate = Date.parse('yyyy-MM-dd', '2016-01-01')

    Patient on(Patient patient, @DelegatesTo(EventsDsl.OnSpec) Closure closure) {
        def eventSaver = { it.save(flush: true, failOnError: true) } as Consumer<PatientEvent>
        def positionSupplier = { PatientEvent.countByAggregate(patient) + 1 }
        def userSupplier = {'anonymous'}
        def dateSupplier = {currDate+=1; currDate}
        new EventsDsl<Patient, Long, PatientEvent>().on(patient, eventSaver, positionSupplier, userSupplier, dateSupplier, closure)
    }

    def destroy = {
    }
}
