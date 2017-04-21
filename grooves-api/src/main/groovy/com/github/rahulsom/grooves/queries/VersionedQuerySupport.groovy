package com.github.rahulsom.grooves.queries

import com.github.rahulsom.grooves.api.AggregateType
import com.github.rahulsom.grooves.api.events.BaseEvent
import com.github.rahulsom.grooves.api.events.DeprecatedBy
import com.github.rahulsom.grooves.api.events.RevertEvent
import com.github.rahulsom.grooves.api.snapshots.VersionedSnapshot
import com.github.rahulsom.grooves.queries.internal.BaseQuery
import com.github.rahulsom.grooves.queries.internal.QueryExecutor
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Observable

/**
 * Aggregate trait that simplifies computing temporal snapshots from events
 *
 * @param <Aggregate>    The Aggregate type`
 * @param <EventType>    The Event type
 * @param <SnapshotType> The snapshot type
 *
 * @author Rahul Somasunderam
 */
@CompileStatic
trait VersionedQuerySupport<
        Aggregate extends AggregateType,
        EventIdType,
        EventType extends BaseEvent<Aggregate, EventIdType, EventType>,
        SnapshotIdType,
        SnapshotType extends VersionedSnapshot<Aggregate, SnapshotIdType, EventIdType, EventType>>
        implements BaseQuery<Aggregate, EventIdType, EventType, SnapshotIdType, SnapshotType> {
    private Logger log = LoggerFactory.getLogger(getClass())

    /**
     *
     * @param aggregate
     * @param maxPosition
     * @return
     */
    private Observable<SnapshotType> getLatestSnapshot(Aggregate aggregate, long maxPosition) {
        getSnapshot(maxPosition, aggregate).
                defaultIfEmpty(createEmptySnapshot()).
                map {
                    log.debug "  -> Last Snapshot: ${it.lastEventPosition ? it : '<none>'}"
                    detachSnapshot(it)

                    it.aggregate = aggregate
                    it
                }
    }

    /**
     * Given a last event, finds the latest snapshot older than that event
     * @param aggregate
     * @param version
     * @return
     */
    private Tuple2<SnapshotType, List<EventType>> getSnapshotAndEventsSince(Aggregate aggregate, long version) {
        getSnapshotAndEventsSince(aggregate, version, version)
    }

    private Tuple2<SnapshotType, List<EventType>> getSnapshotAndEventsSince(Aggregate aggregate, long maxSnapshotPosition, long version) {
        if (maxSnapshotPosition) {
            def lastSnapshot = getLatestSnapshot(aggregate, maxSnapshotPosition).toBlocking().first()

            List<EventType> uncomputedEvents = getUncomputedEvents(aggregate, lastSnapshot, version).toList().toBlocking().first()
            def uncomputedReverts = uncomputedEvents.findAll {
                it instanceof RevertEvent<Aggregate, EventIdType, EventType>
            } as List<RevertEvent>

            if (uncomputedReverts) {
                log.info "     Uncomputed reverts exist: [\n    ${uncomputedEvents.join(',\n    ')}\n]"
                getSnapshotAndEventsSince(aggregate, 0, version)
            } else {
                log.debug "     Events in pair: ${uncomputedEvents*.id}"
                new Tuple2(lastSnapshot, uncomputedEvents)
            }
        } else {
            def lastSnapshot = createEmptySnapshot()

            List<EventType> uncomputedEvents = getUncomputedEvents(aggregate, lastSnapshot, version).toList().toBlocking().first()

            log.debug "     Events in pair: ${uncomputedEvents*.id}"
            new Tuple2(lastSnapshot, uncomputedEvents)
        }
    }

    QueryExecutor<Aggregate, EventIdType, EventType, SnapshotIdType, SnapshotType> getExecutor() {
        new QueryExecutor<Aggregate, EventIdType, EventType, SnapshotIdType, SnapshotType>()
    }

    abstract Observable<EventType> getUncomputedEvents(Aggregate aggregate, SnapshotType lastSnapshot, long version)

    /**
     * Computes a snapshot for specified version of an aggregate
     * @param aggregate The aggregate
     * @param version The version number, starting at 1
     * @param redirect If there has been a deprecation, redirect to the current aggregate's snapshot. Defaults to true.
     * @return An Optional SnapshotType. Empty if cannot be computed.
     */
    Observable<SnapshotType> computeSnapshot(Aggregate aggregate, long version, boolean redirect = true) {

        log.info "Computing snapshot for $aggregate version $version"
        Tuple2<SnapshotType, List<EventType>> seTuple2 = getSnapshotAndEventsSince(aggregate, version)
        def events = seTuple2.second
        def snapshot = seTuple2.first

        if (events.any { it instanceof RevertEvent<Aggregate, EventIdType, EventType> } && snapshot.aggregate) {
            return Observable.empty()
        }
        snapshot.aggregate = aggregate

        Observable<EventType> forwardOnlyEvents = executor.applyReverts(this, Observable.from(events)).
                toList().
                onErrorReturn {
                    executor.applyReverts(
                            this,
                            Observable.from(
                                    getSnapshotAndEventsSince(aggregate, 0, version).second
                            )
                    ).toList().toBlocking().first()
                }.
                flatMap { Observable.from(it) }

        executor.
                applyEvents(this, snapshot, forwardOnlyEvents, [], [aggregate]).
                doOnNext { SnapshotType snapshotType ->
                    if (events) {
                        snapshotType.lastEvent = events.last()
                    }
                    log.info "  --> Computed: $snapshotType"
                }.
                flatMap {
                    def lastEvent = events ? events.last() : null
                    it.deprecatedBy && lastEvent && lastEvent instanceof DeprecatedBy && redirect ?
                            computeSnapshot(it.deprecatedBy, (lastEvent as DeprecatedBy).converse.position) :
                            Observable.just(it)
                }

    }

}
