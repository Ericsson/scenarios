package com.ericsson.de.scenarios.impl;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;

import java.util.Set;

import com.ericsson.de.scenarios.api.Api;
import com.ericsson.de.scenarios.api.ScenarioRunner;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import rx.Observable;

/**
 * In case of exception in Test Step execution - Filters out rx java stacktraces, leaving only user stacktrace.
 */
final class StackTraceFilter {

    private static final Predicate<StackTraceElement> NO_GUAVA = not(HasPackageName.GUAVA);
    private static final Predicate<StackTraceElement> NO_RX_JAVA = not(HasPackageName.RX_JAVA);
    private static final Predicate<StackTraceElement> NO_REFLECTIONS = not(or(HasPackageName.SUN_REFLECT, HasPackageName.JAVA_REFLECT));
    private static final Predicate<StackTraceElement> NO_RX_SCENARIO = not(HasPackageName.RX_SCENARIO);

    static final String RX_SCENARIO_RUNNER = ScenarioRunner.class.getName();
    static final String RX_API = Api.class.getName();

    private static final Set<String> RX_RUNNERS = ImmutableSet.of(RX_SCENARIO_RUNNER, RX_API);

    private StackTraceFilter() {
    }

    static <T extends Throwable> T clearStackTrace(T e) {
        e.setStackTrace(new StackTraceElement[] {});
        return e;
    }

    static <T extends Throwable> T filterFrameworkStackTrace(T e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        int limit = getStackTraceLimit(stackTrace);
        e.setStackTrace(from(stackTrace).limit(limit).filter(NO_RX_JAVA).toArray(StackTraceElement.class));
        if (e.getCause() != null) {
            filterFrameworkStackTrace(e.getCause());
        }
        return e;
    }

    static <T extends Throwable> T filterTestwareStackTrace(T e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        int limit = getStackTraceLimit(stackTrace);
        e.setStackTrace(
                from(stackTrace).limit(limit).filter(NO_RX_JAVA).filter(NO_REFLECTIONS).filter(NO_RX_SCENARIO).toArray(StackTraceElement.class));
        if (e.getCause() != null) {
            filterTestwareStackTrace(e.getCause());
        }
        return e;
    }

    static <T extends Throwable> T filterListenerStackTrace(T e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        int limit = getStackTraceLimit(stackTrace);
        e.setStackTrace(from(stackTrace).limit(limit).filter(NO_GUAVA).filter(NO_RX_JAVA).filter(NO_REFLECTIONS).filter(NO_RX_SCENARIO)
                .toArray(StackTraceElement.class));
        if (e.getCause() != null) {
            filterListenerStackTrace(e.getCause());
        }
        return e;
    }

    @VisibleForTesting
    static int getStackTraceLimit(StackTraceElement[] stackTrace) {
        boolean seenRxRunnerFrame = false;
        for (int frame = 0; frame < stackTrace.length; frame++) {
            String className = stackTrace[frame].getClassName();
            if (RX_RUNNERS.contains(className)) {
                seenRxRunnerFrame = true;
            } else if (seenRxRunnerFrame) {
                String testwarePackage = className.substring(0, className.lastIndexOf("."));
                for (; frame < stackTrace.length; frame++) {
                    className = stackTrace[frame].getClassName();
                    if (!className.startsWith(testwarePackage)) {
                        return frame;
                    }
                }
            }
        }
        return stackTrace.length;
    }

    private static final class HasPackageName implements Predicate<StackTraceElement> {

        static final HasPackageName GUAVA = new HasPackageName("com.google");
        static final HasPackageName RX_JAVA = new HasPackageName(Observable.class.getPackage().getName());
        static final HasPackageName SUN_REFLECT = new HasPackageName("sun.reflect");
        static final HasPackageName JAVA_REFLECT = new HasPackageName("java.lang.reflect");
        static final HasPackageName RX_SCENARIO = new HasPackageName(Implementation.class.getPackage().getName());

        private String packageName;

        private HasPackageName(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public boolean apply(StackTraceElement element) {
            return element.getClassName().startsWith(packageName);
        }
    }
}
