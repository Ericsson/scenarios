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

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Map;

import com.ericsson.de.scenarios.impl.DefaultDataRecordTransformer;
import com.google.common.collect.Maps;

/**
 * Basic HashMap implementation of Data Records.
 * Contains {@link DataRecordBuilder} to facilitate Data Record construction.
 */
public class BasicDataRecord implements DataRecord {
    /**
     * @return builder for BasicDataRecord
     */
    public static DataRecordBuilder builder() {
        return new DataRecordBuilder();
    }

    /**
     * @return builder for BasicDataRecord
     */
    public static DataRecordBuilder copy(DataRecord record) {
        return new DataRecordBuilder(record);
    }

    /**
     * Creates Data Record from Map
     *
     * @return Data Record
     */
    public static DataRecord fromMap(Map<String, Object> values) {
        return new BasicDataRecord(values);
    }

    /**
     * Creates Data Record from name-value pairs.
     * Requires a minimum of two arguments, where first argument represents
     * Field name & second arguments the field value.
     * i.e. BasicDataRecord.fromValues("name1", 1, "name2", 2);
     *
     * @return Data Record
     */
    public static DataRecord fromValues(Object... nameValues) {
        return new BasicDataRecord(nameValues);
    }

    private final Map<String, Object> values = Maps.newHashMap();

    private BasicDataRecord(Map<String, Object> values) {
        this.values.putAll(values);
    }

    private BasicDataRecord(Object... nameValues) {
        checkArgument(nameValues.length % 2 == 0, "Expected: name, value, name, value...");

        for (int i = 0; i < nameValues.length; i += 2) {
            checkArgument(String.class.isInstance(nameValues[i]), "Key must be string");
            values.put((String) nameValues[i], nameValues[i + 1]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(String name) {
        return (T) values.get(name);
    }

    @Override
    public Map<String, Object> getAllFields() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("values", values).toString();
    }

    /**
     * This class allows a user to build a DataRecord, with as many fields as they want.
     */
    public static final class DataRecordBuilder {
        private final Map<String, Object> data = Maps.newHashMap();

        DataRecordBuilder() {
        }

        DataRecordBuilder(DataRecord record) {
            data.putAll(record.getAllFields());
        }

        /**
         * Add a field to the data record
         *
         * @param field
         *         The field identifier
         * @param value
         *         the value for the field
         *
         * @return the builder
         */
        public DataRecordBuilder setField(String field, Object value) {
            data.put(field, value);
            return this;
        }

        /**
         * Add the contents of the map as individual fields to the data record
         *
         * @param map
         *         The Key, Value pairs to add to the data record
         *
         * @return the builder
         */
        public DataRecordBuilder setFields(Map<String, Object> map) {
            data.putAll(map);
            return this;
        }

        /**
         * Add the contents of each of the data records as individual fields to the data record
         *
         * @param records
         *         a list of data records
         *
         * @return the builder
         */
        public DataRecordBuilder setFields(DataRecord... records) {
            for (DataRecord dataRecord : records) {
                setFields(dataRecord.getAllFields());
            }
            return this;
        }

        /**
         * Build the DataRecord
         *
         * @return a DataRecord which contains the fields set in the Builder
         */
        public DataRecord build() {
            return build(DataRecord.class);
        }

        /**
         * Build the DataRecord
         *
         * @param type
         *         subclass of DataRecord representing Data Record fields as getters of Java Bean
         *
         * @return a DataRecord which contains the fields set in the Builder
         */
        public <T extends DataRecord> T build(final Class<T> type) {
            DefaultDataRecordTransformer<T> defaultDataRecordTransformer = new DefaultDataRecordTransformer<>();
            checkArgument(defaultDataRecordTransformer.canTransformTo(type),
                    "Unable to convert to " + type.getSimpleName() + ", as it does not subclass of" + " DataRecord");
            return defaultDataRecordTransformer.transform(new BasicDataRecord(data), type);
        }
    }
}
