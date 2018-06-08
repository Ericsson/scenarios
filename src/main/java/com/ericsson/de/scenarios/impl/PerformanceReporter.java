package com.ericsson.de.scenarios.impl;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.google.common.collect.Iterables.getLast;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;

public class PerformanceReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceReporter.class);

    String summary(String name, List<Internals.TestStepResult> testStepResults) {
        ArrayListMultimap<String, Internals.TestStepResult> resultsByStep = ArrayListMultimap.create();

        long startTime = Long.MAX_VALUE;
        long endTime = 0;
        float successes = 0;
        float failures = 0;

        for (Internals.TestStepResult result : testStepResults) {
            resultsByStep.put(result.id, result);

            if (result.startTime < startTime) {
                startTime = result.startTime;
            }
            if (result.endTime > endTime) {
                endTime = result.endTime;
            }

            if (result.isFailed()) {
                failures++;
            } else {
                successes++;
            }
        }

        StringBuilder builder = new StringBuilder();

        builder.append("\n===============================================================================");
        builder.append("\nRxScenario: ").append(name);
        builder.append(format("%nTest duration: %s", toHumanReadableDuration(endTime - startTime)));
        builder.append(format("%nSamples count: %.0f, %.2f%% (%.0f) failures", successes, failures / (successes == 0 ? 1 : successes), failures));
        builder.append("\nStats by Test Step: ");

        appendTestStepResults(resultsByStep, builder);
        builder.append("\n");

        LOGGER.info(builder.toString());

        return builder.toString();
    }

    private void appendTestStepResults(final ArrayListMultimap<String, Internals.TestStepResult> resultsByStep, final StringBuilder builder) {
        for (Collection<Internals.TestStepResult> stepResults : resultsByStep.asMap().values()) {
            long timeSum = 0;
            long min = Long.MAX_VALUE;
            long max = 0;

            for (Internals.TestStepResult result : stepResults) {
                timeSum += (result.endTime - result.startTime);
                long duration = result.endTime - result.startTime;

                if (duration < min) {
                    min = duration;
                }
                if (duration > max) {
                    max = duration;
                }
            }

            builder.append(format("%n  Name: %s, Samples: %s, Min: %s, Max: %s, Avg: %s", getLast(stepResults).name, stepResults.size(),
                    toHumanReadableDuration(min), toHumanReadableDuration(max), toHumanReadableDuration(timeSum / stepResults.size())));
        }
    }

    private String toHumanReadableDuration(final long millis) {

        final StringBuilder builder = new StringBuilder();
        long acc = millis;
        for (final TimeUnit timeUnit : asList(DAYS, HOURS, MINUTES, SECONDS)) {
            final long convert = timeUnit.convert(acc, MILLISECONDS);
            if (timeUnit.equals(SECONDS)) {
                builder.append(format("%.3fs", (float) acc / 1000));
            } else if (convert > 0) {
                builder.append(convert).append(timeUnit.name().toLowerCase().charAt(0)).append(" ");
                acc -= MILLISECONDS.convert(convert, timeUnit);
            }
        }
        builder.setLength(builder.length() - 1);
        return builder.append("s").toString();
    }
}
