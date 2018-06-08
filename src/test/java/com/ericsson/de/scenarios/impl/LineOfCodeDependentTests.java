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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.RxApi.runnable;

import org.junit.Test;

import com.ericsson.de.scenarios.api.TestStep;

public class LineOfCodeDependentTests {

    @Test
    public void runnable_verifyRunnableTestStepNames() throws Exception {
        TestStep testStepRunnableWithClass = runnable(new TestRunnable());
        TestStep testStepAnonymousRunnable = runnable(new Runnable() {
            @Override
            public void run() {
            }
        });

        assertThat(testStepRunnableWithClass.getName())
                .isEqualTo("LineOfCodeDependentTests$TestRunnable.run(referenced from LineOfCodeDependentTests.java:23)");
        assertThat(testStepAnonymousRunnable.getName())
                .isEqualTo("LineOfCodeDependentTests.runnable_verifyRunnableTestStepNames(LineOfCodeDependentTests.java:24)");
    }

    private static class TestRunnable implements Runnable {

        @Override
        public void run() {
        }
    }
}
