package com.ericsson.de.scenarios.api;

import com.google.common.base.Optional;

/**
 * Encapsulates multiple Data Records from multiple Data Sources in hierarchical structure, including Context and Test Step Params.
 * Used as input value for {@link TestStep}
 *
 * @see com.ericsson.de.scenarios.impl.DataRecords.Parent
 * @see com.ericsson.de.scenarios.impl.DataRecords.Single
 * @see com.ericsson.de.scenarios.impl.DataRecords.Multiple
 */
public interface DataRecordWrapper {

    /**
     * Should return value for parameter injection of {@link TestStep}
     *
     * @param name
     *         of Parameter
     * @param type
     *         of Parameter
     *
     * @return Optional of parameter value
     */
    <V> Optional<V> getFieldValue(String name, Class<V> type);

    /**
     * Gets name of the data source this record belongs to
     *
     * @return name of the data source
     */
    String getDataSourceName();

    /**
     * Gets iteration number of Data Record
     * Parent iterations are divided by <code>.</code>
     * Multiple Data Source iterations divided by <code>-</code>
     */
    String getIteration();
}
