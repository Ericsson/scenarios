package com.ericsson.de.scenarios.impl;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.ericsson.de.scenarios.impl.StackTraceFilter.clearStackTrace;
import static com.google.common.collect.Iterables.getLast;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordTransformer;
import com.ericsson.de.scenarios.api.DataRecordWrapper;

import rx.Observable;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;

/**
 * Applies transformations to Data Records, utilizing RxJava
 */
class RxDataSource {
    /**
     * Combines contents of Data Records
     * E.g. a-b-c + 1-2-3 = a-b-c-1-2-3
     * To append Data Record from Parent Data Source to Data Record from child Data Source
     */
    static Func2<DataRecordWrapper, DataRecordWrapper, DataRecordWrapper> glue() {
        return new Func2<DataRecordWrapper, DataRecordWrapper, DataRecordWrapper>() {
            @Override
            public DataRecordWrapper call(DataRecordWrapper parent, DataRecordWrapper child) {
                return new DataRecords.Parent(child, parent);
            }
        };
    }

    /**
     * Merges contents of Data Records.
     * E.g. a-b-c + a-2-3 = a-b-c-2-3
     * To avoid duplicates in case both Data Records have same parent records.
     */
    static FuncN<DataRecordWrapper> merge() {
        return new FuncN<DataRecordWrapper>() {
            @Override
            public DataRecordWrapper call(Object... args) {
                DataRecordWrapper[] wrappers = Arrays.copyOf(args, args.length, DataRecordWrapper[].class);
                return new DataRecords.Multiple(wrappers);
            }
        };
    }

    /**
     * RxFlow Data Records * Subflow Data Records
     */
    static Observable<DataRecordWrapper> multiply(Observable<DataRecordWrapper> first, final Observable<DataRecordWrapper> second) {
        return first.flatMap(new Func1<DataRecordWrapper, Observable<DataRecordWrapper>>() {
            @Override
            public Observable<DataRecordWrapper> call(final DataRecordWrapper firstDataRecords) {
                return second.map(new Func1<DataRecordWrapper, DataRecordWrapper>() {
                    @Override
                    public DataRecordWrapper call(DataRecordWrapper secondDataRecords) {
                        return new DataRecords.Parent(firstDataRecords, secondDataRecords);
                    }
                });
            }
        });
    }

    /**
     * Repeat Data Source multiple times
     */
    static Observable<DataRecordWrapper> copy(Observable<DataRecordWrapper> dataSource, final int vUsers) {
        return dataSource.flatMap(new Func1<DataRecordWrapper, Observable<DataRecordWrapper>>() {
            @Override
            public Observable<DataRecordWrapper> call(DataRecordWrapper DataRecords) {
                return Observable.just(DataRecords).repeat(vUsers);
            }
        });
    }

    /**
     * Wraps exceptions to CompositeException
     */
    static Throwable compose(Set<Throwable> throwables) {
        throwables.remove(null);
        if (throwables.size() == 1) {
            return throwables.iterator().next();
        } else if (throwables.size() > 1) {
            return clearStackTrace(new CompositeException(throwables));
        }
        return null;
    }

    static Func1<DataRecord, DataRecordWrapper> wrapDataRecords(final String name, final DataRecordTransformer transformer) {
        return new Func1<DataRecord, DataRecordWrapper>() {
            final AtomicInteger iteration = new AtomicInteger();

            @Override
            public DataRecordWrapper call(DataRecord testDataRecord) {
                return new DataRecords.Single(name, transformer, iteration.incrementAndGet(), testDataRecord);
            }
        };
    }

    static long startTime(Collection<Internals.Exec> executions) {
        long min = Long.MAX_VALUE;

        for (Internals.Exec execution : executions) {
            if (execution.getExecutedTestSteps().get(0).startTime < min) {
                min = execution.getExecutedTestSteps().get(0).startTime;
            }
        }

        return min;
    }

    public static long endTime(Collection<Internals.Exec> executions) {
        long max = 0;

        for (Internals.Exec execution : executions) {
            if (getLast(execution.getExecutedTestSteps()).endTime > max) {
                max = getLast(execution.getExecutedTestSteps()).endTime;
            }
        }

        return max;
    }
}
