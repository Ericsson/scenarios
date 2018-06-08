package com.ericsson.de.scenarios.testware;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.api.RxApiImpl.flow;
import static com.ericsson.de.scenarios.api.RxApiImpl.scenario;
import static com.ericsson.de.scenarios.impl.RxApi.fromCsv;

import java.util.Stack;
import javax.inject.Named;

import org.junit.Test;

import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.RxApiImpl;
import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.impl.ScenarioTest;

public class CsvDataSourceTest extends ScenarioTest {

    private static final Stack<String> STACK = new Stack<>();

    @Test
    public void csvDataRecords() throws Exception {
        Scenario scenario = scenario().addFlow(flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            public void testStep(@Named("username") String username) {
                STACK.push(username);
            }
        }).withDataSources(fromCsv("testDs", "csv/fromCsvTest.csv", DataRecord.class))).build();

        RxApiImpl.run(scenario);

        assertThat(STACK).containsExactly("John", "Mike");
    }
}
