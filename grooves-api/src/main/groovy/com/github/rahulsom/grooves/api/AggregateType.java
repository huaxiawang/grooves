package com.github.rahulsom.grooves.api;

/**
 * Marks a class as an aggregate.
 *
 * <p>You should not have to use this class.
 *
 * @param <AggregateIdT> The type of the primary key/identifier for the Aggregate
 *
 * @author Rahul Somasunderam
 */
public interface AggregateType<AggregateIdT> {
    AggregateIdT getId();

    void setId(AggregateIdT id);
}
