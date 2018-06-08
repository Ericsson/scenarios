/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package com.ericsson.de.scenarios.impl;

import java.util.Iterator;

import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataSource;
import com.google.common.base.Preconditions;

/**
 * {@link DataSource} of iterable of {@link DataRecord}
 */
class RxIterableDataSource<T> extends DataSource<T> {
    private Iterable<? extends DataRecord> iterable;

    RxIterableDataSource(String name, Class<?> type, Iterable<? extends DataRecord> iterable) {
        super(name, type);
        this.iterable = iterable;
    }

    RxIterableDataSource(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    public Iterator<? extends DataRecord> getIterator() {
        Preconditions.checkNotNull(iterable, "Iterator should be passed in constructor or overriden");
        return iterable.iterator();
    }

    @Override
    public DataSource<T> newDefinition() {
        return new RxIterableDataSource<>(name, getType(), this);
    }
}
