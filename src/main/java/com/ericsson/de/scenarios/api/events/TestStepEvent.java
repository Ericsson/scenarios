package com.ericsson.de.scenarios.api.events;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.TestStep;

public class TestStepEvent extends Event {

    private final String name;

    private TestStepEvent(TestStep testStep) {
        this.name = testStep.getName();
    }

    public String getName() {
        return name;
    }

    public static class TestStepStartedEvent extends TestStepEvent {

        private DataRecordWrapper dataRecord;

        public TestStepStartedEvent(TestStep testStep, DataRecordWrapper dataRecord) {
            super(testStep);
            this.dataRecord = dataRecord;
        }

        public DataRecordWrapper getDataRecord() {
            return dataRecord;
        }
    }

    public static class TestStepFinishedEvent extends TestStepEvent {

        private TestStep.Status status;
        private Throwable error;

        public TestStepFinishedEvent(TestStep testStep, TestStep.Status status, Throwable error) {
            super(testStep);
            this.status = status;
            this.error = error;
        }

        public TestStep.Status getStatus() {
            return status;
        }

        public Throwable getError() {
            return error;
        }
    }
}
