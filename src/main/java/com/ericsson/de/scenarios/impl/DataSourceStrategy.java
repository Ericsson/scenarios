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

import static java.lang.String.format;

import static com.ericsson.de.scenarios.impl.StackTraceFilter.filterFrameworkStackTrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.DataSource;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

public abstract class DataSourceStrategy {
    public static final String ERROR_DATA_SOURCE_EMPTY = "Data Source `%s` did not produce any Data Records";
    private static final DataRecord EMPTY = BasicDataRecord.builder().build();

    private final Observable<DataRecordWrapper> dataSource;
    private final String definition;
    final int vUsers;

    DataSourceStrategy(Observable<DataRecordWrapper> dataSource, String definition, int vUsers) {
        this.dataSource = dataSource;
        this.definition = definition;
        this.vUsers = vUsers;
    }

    /**
     * Provide Data Source Observable
     */
    abstract Observable<DataRecordWrapper> provide();

    /**
     * Merge Data Records with {@param parentDataRecords}
     */
    abstract Observable<DataRecordWrapper> forkFrom(Observable<DataRecordWrapper> parentDataRecords);

    /**
     * @return copy if Data Source is mutable to avoid modification in real time
     */
    Observable<DataRecordWrapper> getDataSource() {
        if (dataSource instanceof ReplaySubject) {
            ReplaySubject replaySubject = ReplaySubject.class.cast(dataSource);
            Object[] values = replaySubject.getValues();
            DataRecordWrapper[] dataRecords = Arrays.copyOf(values, values.length, DataRecordWrapper[].class);
            return Observable.from(dataRecords);
        }

        return dataSource;
    }

    static DataSourceStrategy fromDefinitions(DataSource[] definitions, int vUsers) {
        if (definitions.length == 1) {
            return fromDefinition(definitions[0], vUsers);
        } else {
            List<DataSourceStrategy> strategies = new ArrayList<>(definitions.length);
            for (DataSource definition : definitions) {
                strategies.add(fromDefinition(definition, vUsers));
            }

            return new Multiple(strategies, vUsers);
        }
    }

    static DataSourceStrategy fromDefinition(DataSource<DataRecord> definition, int vUsers) {
        Observable<DataRecordWrapper> observable = provideObservable(definition);

        return definition.isShared() ? new Shared(observable, definition.toString(), vUsers) : new Copied(observable, definition.toString(), vUsers);
    }

    private static Observable<DataRecordWrapper> provideObservable(DataSource<DataRecord> definition) {
        Observable<DataRecordWrapper> observable = Observable.from((Iterable<DataRecord>) definition).defaultIfEmpty(EMPTY)
                .doOnNext(errorOnEmpty(definition.getName()))
                .map(RxDataSource.wrapDataRecords(definition.getName(), definition.getDataRecordTransformer())).cache();

        if (definition.isCyclic()) {
            observable = makeCyclic(observable);
        }

        return observable;
    }

    private static Action1<? super DataRecord> errorOnEmpty(final String name) {
        return new Action1<DataRecord>() {
            @Override
            public void call(DataRecord dataRecord) {
                if (dataRecord == EMPTY) {
                    throw filterFrameworkStackTrace(new IllegalArgumentException(format(ERROR_DATA_SOURCE_EMPTY, name)));
                }
            }
        };
    }

    /**
     * Subscription should be done on separate thread to avoid deadlock in {@link RxDataSource#merge()} while
     * calling {@link Observable#unsafeSubscribe} on multiple repeating observables
     */
    private static <T> Observable<T> makeCyclic(Observable<T> observable) {
        return observable.observeOn(Schedulers.computation()).repeat();
    }

    static DataSourceStrategy empty(String name, int vUsers) {
        return new Empty(name, vUsers);
    }

    String definition() {
        return definition;
    }

    private static class Multiple extends DataSourceStrategy {
        final private List<DataSourceStrategy> strategies;

        Multiple(List<DataSourceStrategy> strategies, int vUsers) {
            super(null, "multiple Data Sources", vUsers);
            this.strategies = strategies;
        }

        @Override
        Observable<DataRecordWrapper> provide() {
            List<Observable<DataRecordWrapper>> observables = new ArrayList<>(strategies.size());
            for (DataSourceStrategy strategy : strategies) {
                observables.add(strategy.provide());
            }

            return Observable.zip(observables, RxDataSource.merge());
        }

        @Override
        Observable<DataRecordWrapper> forkFrom(Observable<DataRecordWrapper> parentDataRecords) {
            List<Observable<DataRecordWrapper>> observables = new ArrayList<>(strategies.size());
            for (DataSourceStrategy strategy : strategies) {
                observables.add(strategy.forkFrom(parentDataRecords));
            }

            return Observable.zip(observables, RxDataSource.merge());
        }
    }

    private static class Shared extends DataSourceStrategy {
        Shared(Observable<DataRecordWrapper> dataSource, String definition, int vUsers) {
            super(dataSource, definition, vUsers);
        }

        @Override
        Observable<DataRecordWrapper> provide() {
            return getDataSource();
        }

        @Override
        Observable<DataRecordWrapper> forkFrom(Observable<DataRecordWrapper> parentDataRecords) {
            Observable<DataRecordWrapper> idealRepetitions = RxDataSource.copy(parentDataRecords, vUsers);

            return Observable.zip(idealRepetitions.repeat(), getDataSource(), RxDataSource.glue());
        }
    }

    private static class Copied extends DataSourceStrategy {
        Copied(Observable<DataRecordWrapper> dataSource, String definition, int vUsers) {
            super(dataSource, definition, vUsers);
        }

        @Override
        Observable<DataRecordWrapper> provide() {
            return RxDataSource.copy(getDataSource(), vUsers);
        }

        @Override
        Observable<DataRecordWrapper> forkFrom(Observable<DataRecordWrapper> parentDataRecords) {
            return RxDataSource.multiply(provide(), parentDataRecords);
        }
    }

    static class Empty extends Copied {

        Empty(String name, int vUsers) {
            super(Observable.<DataRecordWrapper>just(new DataRecords.Empty(name)), "", vUsers);
        }
    }
}
