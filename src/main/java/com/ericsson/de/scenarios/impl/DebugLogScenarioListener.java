package com.ericsson.de.scenarios.impl;

import static java.lang.Thread.currentThread;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.api.events.ScenarioEvent;
import com.google.common.eventbus.Subscribe;

/**
 * Implements the debug logging listeners for RxScenario, RxFlow & Test Step
 */
@SuppressWarnings("unused")
public class DebugLogScenarioListener implements RxScenarioListener {

    public static final RxScenarioListener INSTANCE = new DebugLogScenarioListener();

    private static final Logger logger = LoggerFactory.getLogger(DebugLogScenarioListener.class);

    private static final int DATA_RECORDS_LIMIT = 10;

    private DebugLogScenarioListener() {
    }

    @Subscribe
    public void debugScenario(ScenarioEvent.ScenarioStartedEvent event) {
        Scenario scenario = event.getScenario();
        logger.info("Running RxScenario '{}'", scenario.getName());
    }

    @Subscribe
    public void debugFlow(ScenarioEventBus.InternalFlowStartedEvent event) {
        RxFlow rxFlow = event.getFlow();
        DataSourceStrategy dataSource = rxFlow.dataSource;
        if (DataSourceStrategy.Empty.class.isAssignableFrom(dataSource.getClass())) {
            logger.info("Running RxFlow '{}' without Data Source", rxFlow.getName());
        } else {
            List<DataRecordWrapper> dataRecords = event.getDataSource().toList().toBlocking().single();
            logger.info("Running RxFlow '{}' with {} consisting of {} Data Records: {}", rxFlow.getName(), dataSource.definition(),
                    dataRecords.size(), samples(dataRecords));
        }
    }

    private String samples(List<DataRecordWrapper> dataRecords) {
        if (dataRecords.size() > DATA_RECORDS_LIMIT) {
            String temp = dataRecords.subList(0, DATA_RECORDS_LIMIT).toString();
            return temp.substring(0, temp.length() - 1) + "...]";
        } else {
            return dataRecords.toString();
        }
    }

    @Subscribe
    public void debugTestStep(ScenarioEventBus.InternalTestStepStartedEvent event) {
        Internals.Exec execution = event.getExecution();
        String oldThreadName = currentThread().getName();
        currentThread().setName(execution.flowPath + ".vUser-" + execution.vUser);
        logger.info("Running Test Step '{}' with Data Record {} and context {}", event.getName(), execution.dataRecord, execution.context.values);
        currentThread().setName(oldThreadName);
    }
}
