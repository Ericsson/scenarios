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

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Proxy;
import java.util.Iterator;

import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataSource;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;

/**
 * {@link DataSource} of iterable of {@link Object}
 */
class RxObjectDataSource<T> extends DataSource<T> {
    private final Iterable<T> iterable;

    RxObjectDataSource(String name, Iterable<T> iterable) {
        super(name, getType(iterable));
        this.iterable = iterable;
    }

    /**
     * Note that proxy may be passed here
     *
     * @see DefaultDataRecordTransformer
     */
    private static <T> Class<?> getType(Iterable<T> iterable) {
        checkArgument(iterable.iterator().hasNext());
        T next = iterable.iterator().next();
        Class<?> type = next.getClass();
        return Proxy.isProxyClass(type) ? type.getInterfaces()[0] : type;
    }

    @Override
    public Iterator<? extends DataRecord> getIterator() {
        return Iterators.transform(iterable.iterator(), new Function<T, DataRecord>() {
            @Override
            public DataRecord apply(Object value) {
                return BasicDataRecord.fromValues(name, value);
            }
        });
    }

    @Override
    public DataSource<T> newDefinition() {
        return new RxObjectDataSource<>(name, iterable);
    }
}
