package com.ericsson.de.scenarios;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.ReplaySubject;

public class RxJavaExploratoryTest {
    @Test
    public void twoDataSources() throws Exception {
        Observable<Integer> dataSource = Observable.just(1, 2);
        Observable<String> dataSource2 = Observable.just("a", "b", "c", "d");

        Observable.zip(dataSource2, dataSource, new Func2<String, Integer, Object>() {
            @Override
            public Object call(String integer, Integer integer2) {
                return "" + integer2 + integer;
            }
        }).subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void twoDataSource2s() throws Exception {
        Observable.just(1, 2, 3, 4).map(new Func1<Integer, String>() {
            @Override
            public String call(Integer item) {
                return item.toString();
            }
        }).map(new Func1<String, String>() {
            @Override
            public String call(String item) {
                return "<" + item + ">";
            }
        }).subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void filterDataSources() throws Exception {
        Observable<Integer> dataSource = Observable.just(1, 2, 3, 4);

        final AtomicInteger runtime = new AtomicInteger(0);

        Observable<Integer> filteredDataSource = dataSource.filter(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer integer) {
                return integer > runtime.get();
            }
        });

        runtime.set(2);

        filteredDataSource.subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void testEmpty() throws Exception {
        Observable.empty().doOnNext(new ThreadNamePrinterAction("post")).subscribe();
    }

    @Test
    public void skipOnError() throws Exception {
        Observable.range(1, 5).filter(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer e) {
                return !e.equals(3);
            }
        }).doOnNext(new ThreadNamePrinterAction("post")).subscribe();
    }

    @Test
    public void stopOnError() throws Exception {
        Observable.range(1, 5).takeWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer e) {
                return e < 3;
            }
        }).doOnNext(new ThreadNamePrinterAction("post")).subscribe();
    }

    @Test
    public void stopOnError2() throws Exception {
        Observable.range(1, 5).doOnNext(new ThreadNamePrinterAction("pre")).takeWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer e) {
                return e < 3;
            }
        }).doOnNext(new ThreadNamePrinterAction("post")).subscribe();
    }

    @Test
    public void stopOnError3() throws Exception {
        final BehaviorSubject<Object> stopper = BehaviorSubject.create();

        Observable.range(1, 5).takeUntil(stopper).doOnNext(new ThreadNamePrinterAction("pre")).doOnNext(new ThreadNamePrinterAction("post"))
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer e) {
                        if (e == 3) {
                            stopper.onNext(new Object());
                        }
                    }
                }).subscribe();
    }

    @Test
    public void forkJoin() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        final Scheduler scheduler = Schedulers.from(executorService);

        Observable.range(1, 5).flatMap(new Func1<Integer, Observable<?>>() {
            @Override
            public Observable<?> call(Integer e) {
                return Observable.just(e).subscribeOn(scheduler).doOnNext(new ThreadNamePrinterAction("shiftedUp"));
            }
        }).toBlocking().subscribe(new ThreadNamePrinterAction("end"));
    }

    @Test
    public void forkJoin2() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        final Scheduler scheduler = Schedulers.from(executorService);

        BehaviorSubject<Object> join = BehaviorSubject.create();

        join.subscribeOn(Schedulers.immediate()).observeOn(Schedulers.immediate()).doOnNext(new ThreadNamePrinterAction("processing"))
                .subscribe(new ThreadNamePrinterAction("end"));

        Observable.range(1, 5).flatMap(new Func1<Integer, Observable<?>>() {
            @Override
            public Observable<?> call(Integer e) {
                return Observable.just(e).doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer z) {
                        System.out.println("shiftedUp " + z + " " + Thread.currentThread().getName());
                        RxJavaExploratoryTest.this.sleep();
                    }
                }).subscribeOn(scheduler);
            }
        }).subscribe(join);
    }

    @Test
    public void forkJoin3() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        final Scheduler scheduler = Schedulers.from(executorService);

        Observable.range(1, 10).doOnNext(new PrinterAction("beforeFlatMap")).flatMap(new Func1<Integer, Observable<?>>() {
            @Override
            public Observable<?> call(Integer e) {
                return Observable.just(e).subscribeOn(scheduler).doOnNext(sleep2(10)).doOnNext(new ThreadNamePrinterAction("shiftedUp"));
            }
        }, 2) // ← Limit parallel executions (VUsers)
                .toBlocking().subscribe();
    }

    private Action1<? super Object> sleep2(int i) {
        return new Action1<Object>() {
            @Override
            public void call(Object o) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Test
    public void passData() throws Exception {
        ReplaySubject<String> outputDataSource = ReplaySubject.create();
        outputDataSource.onNext("Added");
        outputDataSource.subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void passDataConcurrent() throws Exception {
        ReplaySubject<Object> objectReplaySubject = ReplaySubject.create();
        objectReplaySubject.onNext("1");
        objectReplaySubject.onNext("2");
        objectReplaySubject.onNext("3");
        objectReplaySubject.onCompleted(); //!mandatory
        objectReplaySubject.toBlocking().subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void passDataConcurrentEmpty() throws Exception {
        ReplaySubject<Object> objectReplaySubject = ReplaySubject.create();
        objectReplaySubject.onCompleted(); //!mandatory
        objectReplaySubject.toBlocking().subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void passDataConcurrentMultiple() throws Exception {
        ReplaySubject<Object> objectReplaySubject = ReplaySubject.create();
        objectReplaySubject.onNext("1");
        objectReplaySubject.onNext("2");

        Observable<Object> copy1 = Observable.from(objectReplaySubject.getValues());
        copy1.toBlocking().subscribe(PrinterAction.INSTANCE);

        objectReplaySubject.onNext("3");

        Observable<Object> copy2 = Observable.from(objectReplaySubject.getValues());
        copy2.toBlocking().subscribe(PrinterAction.INSTANCE);
    }

    @Test
    public void doCallIteratorOnlyOnce() throws Exception {
        Iterable<Integer> callMeOnce = new Iterable<Integer>() {
            boolean firstCall = true;

            @Override
            public Iterator<Integer> iterator() {
                Preconditions.checkArgument(firstCall, "multiple calls detected");
                firstCall = false;
                return Lists.newArrayList(1, 2).iterator();
            }
        };

        Observable<Integer> observable = Observable.from(callMeOnce).cache();

        observable.subscribe(PrinterAction.INSTANCE);
        observable.subscribe(PrinterAction.INSTANCE);
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class PrinterAction implements Action1<Object> {

        public static final PrinterAction INSTANCE = new PrinterAction();
        private String name;

        public PrinterAction(String name) {
            this.name = name;
        }

        public PrinterAction() {
            this("");
        }

        @Override
        public void call(Object o) {
            System.out.println(name + o);
        }
    }

    public static class ThreadNamePrinterAction implements Action1<Object> {

        private String prefix;

        ThreadNamePrinterAction(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void call(Object o) {
            System.out.println(prefix + " " + o + " " + Thread.currentThread().getName());
        }
    }
}
