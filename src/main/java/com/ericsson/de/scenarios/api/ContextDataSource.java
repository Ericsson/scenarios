package com.ericsson.de.scenarios.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.de.scenarios.impl.Bridge;

/**
 * Only mutable implementation of Data Source in Rx RxScenario.
 * Intended for collecting Test Step results. Allows reuse of collected results in scope of one RxScenario.
 */
public class ContextDataSource<T> extends DataSource<T> {

    private static final String ERROR_NOT_POPULATED = "Data Source %s was not populated during scenario execution";

    private ConcurrentLinkedQueue<DataRecord> results = new ConcurrentLinkedQueue<>();
    private AtomicBoolean populated = new AtomicBoolean();

    ContextDataSource(String name, Class<T> type) {
        super(name, type);
    }

    private ContextDataSource(String name, Class<T> type, AtomicBoolean populated, ConcurrentLinkedQueue<DataRecord> results) {
        super(name, type);
        this.results = results;
        this.populated = populated;
    }

    /**
     * @return Iterator of DataRecord type.
     */
    @Override
    public Iterator<? extends DataRecord> getIterator() {
        Bridge.checkRxState(populated.get(), ERROR_NOT_POPULATED, name);
        return results.iterator();
    }

    @Override
    protected DataSource<T> newDefinition() {
        return new ContextDataSource<>(name, (Class<T>) getType(), populated, results);
    }

    @SuppressWarnings("unchecked")
    void collectFromResult(String name, Object result) {
        populated.set(true);
        if (result instanceof DataRecord) {
            DataRecord dataRecord = DataRecord.class.cast(result);
            if (dataRecordIsWrapped(name, dataRecord)) {
                collectFromResult(name, dataRecord.getFieldValue(name));
            } else {
                results.add(dataRecord);
            }
        } else if (Bridge.isCollectionOfDataRecords(result)) {
            results.addAll(Collection.class.cast(result));
        } else {
            DataRecord record = BasicDataRecord.fromValues(name, result);
            results.add(record);
        }
    }

    /**
     * Test Step result might be wrapped in another DataRecord
     */
    private boolean dataRecordIsWrapped(String name, DataRecord dataRecord) {
        return dataRecord.getFieldValue(name) != null;
    }
}
