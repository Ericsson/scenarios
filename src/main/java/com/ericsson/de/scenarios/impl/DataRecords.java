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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordTransformer;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

/**
 * Implementations of different level {@link DataRecordWrapper} strategies
 *
 * @see Single
 * @see Multiple
 * @see Parent
 */
abstract class DataRecords implements DataRecordWrapper {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(DataRecords.class);
    final String dataSourceName;

    DataRecords(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    /**
     * Returns the data source field value
     * Should support full paths, i.e. `DataSourceName.DataRecordName`
     * Can return implementation of DataRecord if {@param name} == DataSourceName
     *
     * @param name
     *         field name
     *
     * @return field value
     */
    public abstract <V> Optional<V> getFieldValue(String name, Class<V> type);

    /**
     * Gets name of the data source this record belongs to
     *
     * @return name of the data source
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    private static String toJSON(DataRecord dataRecord) {
        Map<String, Object> values = new TreeMap<>(dataRecord.getAllFields());
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            LOGGER.error("Couldn't write dataRecord as a json string so returning it's toString representation", e);
            return values.toString();
        }
    }

    static class Single extends DataRecords {
        private final DataRecordTransformer transformer;
        private final int iteration;
        private final DataRecord dataRecord;

        Single(String dataSourceName, DataRecordTransformer transformer, int iteration, DataRecord dataRecord) {
            super(dataSourceName);
            this.transformer = transformer;
            this.iteration = iteration;
            this.dataRecord = dataRecord;
        }

        @Override
        public <V> Optional<V> getFieldValue(String name, Class<V> type) {
            String[] fullName = name.split("\\.", 2);
            if (fullName.length == 1) {
                if (dataSourceName.equals(name) && transformer.canTransformTo(type)) {
                    return transform(type);
                } else {
                    return fromDataRecord(name, type);
                }
            } else if (fullName.length == 2) {
                if (dataSourceName.equals(fullName[0])) {
                    return fromDataRecord(fullName[1], type);
                } else {
                    return fromDataRecord(name, type);
                }
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public String getIteration() {
            return "" + iteration;
        }

        @SuppressWarnings("unchecked")
        private <V> Optional<V> transform(Class<V> type) {
            return Optional.of((V) transformer.transform(dataRecord, type));
        }

        private <V> Optional<V> fromDataRecord(String name, Class<V> type) {
            Object value = fromDataRecordRecursively(dataRecord, name, type);
            if (value != null) {
                return Optional.of(type.cast(value));
            } else {
                return Optional.absent();
            }
        }

        private Object fromDataRecordRecursively(DataRecord dataRecord, String name, Class<?> type) {
            String[] fullName = name.split("\\.", 2);
            if (fullName.length == 1) {
                Object value = dataRecord.getFieldValue(name);
                return transformer.convert(name, value, type);
            } else if (fullName.length == 2) {
                Object value = dataRecord.getFieldValue(fullName[0]);
                if (value != null && value instanceof DataRecord) {
                    return fromDataRecordRecursively(DataRecord.class.cast(value), fullName[1], type);
                }
            }

            return null;
        }

        @Override
        public String toString() {
            return "{\"" + dataSourceName + "\":" + DataRecords.toJSON(dataRecord) + '}';
        }
    }

    static class Empty extends DataRecords {
        Empty(String dataSourceName) {
            super(dataSourceName);
        }

        @Override
        public <V> Optional<V> getFieldValue(String name, Class<V> type) {
            return Optional.absent();
        }

        @Override
        public String toString() {
            return "{}";
        }

        @Override
        public String getIteration() {
            return "0";
        }
    }

    static class Forbidden extends DataRecords {

        Forbidden(String dataSourceName) {
            super(dataSourceName);
        }

        @Override
        public <V> Optional<V> getFieldValue(String name, Class<V> type) {
            String message = "Current test step shouldn't accept any arguments (e.g. Before or After rxFlow test steps)";
            throw new IllegalArgumentException(message);
        }

        @Override
        public String getIteration() {
            return "0";
        }
    }

    static class Multiple extends DataRecords {
        final List<DataRecordWrapper> dataRecords;

        Multiple(DataRecordWrapper... dataRecords) {
            super("multiple");
            this.dataRecords = Arrays.asList(dataRecords);
        }

        @Override
        public <V> Optional<V> getFieldValue(String name, Class<V> type) {
            for (DataRecordWrapper dataRecord : dataRecords) {
                Optional<V> fieldValue = dataRecord.getFieldValue(name, type);
                if (fieldValue.isPresent()) {
                    return fieldValue;
                }
            }
            return Optional.absent();
        }

        @Override
        public String getIteration() {
            return Joiner.on("-").join(Iterables.transform(dataRecords, new Function<DataRecordWrapper, String>() {
                @Override
                public String apply(DataRecordWrapper dataRecord) {
                    return dataRecord.getIteration();
                }
            }));
        }

        @Override
        public String toString() {
            return dataRecords.toString();
        }
    }

    static class Parent extends DataRecords {
        final DataRecordWrapper child;
        final DataRecordWrapper parent;

        Parent(DataRecordWrapper child, DataRecordWrapper parent) {
            super(child.getDataSourceName());
            this.child = child;
            this.parent = parent;
        }

        @Override
        public <V> Optional<V> getFieldValue(String name, Class<V> type) {
            Optional<V> fieldValue = child.getFieldValue(name, type);
            if (fieldValue.isPresent()) {
                return fieldValue;
            }

            fieldValue = parent.getFieldValue(name, type);
            if (fieldValue.isPresent()) {
                return fieldValue;
            }

            return Optional.absent();
        }

        @Override
        public String getIteration() {
            return parent.getIteration() + "." + child.getIteration();
        }

        @Override
        public String toString() {
            return "[" + child + "," + parent + ']';
        }
    }

    static class Parameter extends Parent {

        Parameter(DataRecordWrapper dataRecord, Map<String, Object> parameters) {
            super(wrapParameters(parameters), dataRecord);
        }

        private static DataRecordWrapper wrapParameters(Map<String, Object> parameters) {
            Map<String, Map<String, Object>> dataSources = groupByDataSourceName(parameters);
            DataRecordWrapper[] wrappers = FluentIterable.from(dataSources.entrySet())
                    .transform(new Function<Map.Entry<String, Map<String, Object>>, DataRecordWrapper>() {
                        @Override
                        public DataRecordWrapper apply(Map.Entry<String, Map<String, Object>> input) {
                            return wrapParameter(input.getKey(), input.getValue());
                        }
                    }).toArray(DataRecordWrapper.class);
            return wrappers.length == 1 ? wrappers[0] : new Multiple(wrappers);
        }

        private static Map<String, Map<String, Object>> groupByDataSourceName(Map<String, Object> parameters) {
            Map<String, Map<String, Object>> dataSources = newLinkedHashMap();
            for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
                addParameter(dataSources, parameter);
            }
            return dataSources;
        }

        private static void addParameter(Map<String, Map<String, Object>> dataSources, Map.Entry<String, Object> parameter) {
            String name = parameter.getKey();
            String[] fullName = name.split("\\.");
            checkArgument(fullName.length <= 2, "Invalid Data Record reference: %s", name);
            String dataSourceName = (fullName.length == 2) ? fullName[0] : name;
            String parameterName = (fullName.length == 2) ? fullName[1] : name;
            dataSourceParameters(dataSources, dataSourceName).put(parameterName, parameter.getValue());
        }

        private static Map<String, Object> dataSourceParameters(Map<String, Map<String, Object>> dataSources, String dataSourceName) {
            Map<String, Object> dataSourceParameters = dataSources.get(dataSourceName);
            if (dataSourceParameters == null) {
                dataSourceParameters = newLinkedHashMap();
                dataSources.put(dataSourceName, dataSourceParameters);
            }
            return dataSourceParameters;
        }

        private static DataRecordWrapper wrapParameter(String name, Map<String, Object> values) {
            return new Single(name, new DefaultDataRecordTransformer(), 0, BasicDataRecord.fromMap(values));
        }
    }

    static class Binding extends DataRecords {
        private final DataRecordWrapper source;
        private final Map<String, String> bindings;

        Binding(DataRecordWrapper source, Map<String, String> bindings) {
            super("bindings");
            this.source = source;
            this.bindings = bindings;
        }

        @Override
        public <V> Optional<V> getFieldValue(String name, Class<V> type) {
            if (bindings.containsKey(name)) {
                name = bindings.get(name);
            }

            return source.getFieldValue(name, type);
        }

        @Override
        public String getIteration() {
            return "0";
        }
    }
}
