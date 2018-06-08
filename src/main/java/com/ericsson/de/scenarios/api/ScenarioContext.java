package com.ericsson.de.scenarios.api;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

/**
 * Used for Injection of ScenarioContext in {@link TestStep}
 * I.e. <code>@Input(CONTEXT_RECORD_NAME) ScenarioContext context</code>
 */
public interface ScenarioContext extends DataRecord {
    String CONTEXT_RECORD_NAME = "scenarioContext";
}
