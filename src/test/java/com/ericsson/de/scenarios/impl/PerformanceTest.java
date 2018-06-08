package com.ericsson.de.scenarios.impl;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.api.Api.runner;
import static com.ericsson.de.scenarios.impl.RxApi.during;
import static com.ericsson.de.scenarios.impl.RxApi.fromIterable;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Named;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;

public class PerformanceTest {
    @Test
    @Ignore("depends on concurrency")
    public void testRampup() throws Exception {
        int rampUpTime = 9;
        int vUsers = 3;

        final Stack<String> stack = new Stack<>();
        final ConcurrentHashMap<String, Long> times = new ConcurrentHashMap<>();

        final Stopwatch timer = Stopwatch.createUnstarted();

        PerformanceFlowBuilder flow = (PerformanceFlowBuilder) new PerformanceFlowBuilder("test").addTestStep(new ScenarioTest.InlineInvocation() {
            public void validateSubFlow1(@Named("name") Integer i) throws Exception {
                String name = Thread.currentThread().getName().replaceAll("pool-\\d-", "") + " dr-" + i;
                stack.push(name);
                times.put(name, timer.elapsed(SECONDS));
                System.out.println("start " + name + " " + timer.elapsed(SECONDS) + " sec");
                sleep(1000L);
                System.out.println(" stop " + name + " " + timer.elapsed(SECONDS) + " sec");
            }
        }).withVUsers(vUsers).withDataSources(fromIterable("name", asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).shared());

        flow.withRampUp(RxRampUp.during(rampUpTime, SECONDS));

        timer.start();
        RxScenarioRunner build = runner().build();
        build.runPerformance(flow);

        assertThat(stack).containsExactly("thread-1 dr-1", "thread-1 dr-4", "thread-1 dr-5", "thread-2 dr-2", "thread-1 dr-6", "thread-2 dr-7",
                "thread-1 dr-8", "thread-2 dr-9", "thread-3 dr-3");

        int rampupInterval = 9 / 3;
        assertThat(times.get("thread-2 dr-2")).isGreaterThanOrEqualTo(rampupInterval);
        assertThat(times.get("thread-3 dr-3")).isGreaterThanOrEqualTo(rampupInterval * 2);
    }

    @Test
    @Ignore("depends on concurrency")
    public void testDuration() throws Exception {
        int vUsers = 3;
        int duration = 10;
        final Stack<String> stack = new Stack<>();

        PerformanceFlowBuilder flow = (PerformanceFlowBuilder) new PerformanceFlowBuilder("test").addTestStep(new ScenarioTest.InlineInvocation() {
            public void validateSubFlow1(@Named("name") Integer i) throws Exception {
                stack.add("" + i);
                sleep(1000L);
            }
        }).runWhile(during(duration, SECONDS)).withVUsers(vUsers).withDataSources(fromIterable("name", asList(1, 2, 3)).shared());

        final Stopwatch timer = Stopwatch.createStarted();
        RxScenarioRunner build = runner().build();
        build.runPerformance(flow);

        assertThat(timer.elapsed(SECONDS)).isGreaterThanOrEqualTo(duration);

        assertThat(HashMultiset.create(stack).count("1")).isGreaterThan(3);
        assertThat(HashMultiset.create(stack).count("2")).isGreaterThan(3);
        assertThat(HashMultiset.create(stack).count("3")).isGreaterThan(3);
    }
}
