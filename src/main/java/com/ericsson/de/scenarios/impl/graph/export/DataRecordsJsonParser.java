package com.ericsson.de.scenarios.impl.graph.export;

import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Iterator;
import java.util.Map;

import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Parses JSON output produced by {@link com.ericsson.de.scenarios.impl.DataRecords} children {@code toString()}
 * into a {@code Map<String, DataRecord>}, where keys are Data Source names,
 * and values are Data Records
 */
public class DataRecordsJsonParser {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parses Data Records into filtered structure to assist with Graph Generation.
     *
     * @param dataRecord
     *         The DataRecord to be parsed
     *
     * @return Parsed Map
     */
    public Map<String, DataRecord> parseDataRecords(String dataRecord) {
        Map<String, DataRecord> dataRecords = newLinkedHashMap();
        JsonNode root = readDataRecordTree(dataRecord);
        parseNode(dataRecords, root);
        return dataRecords;
    }

    private JsonNode readDataRecordTree(String dataRecord) {
        try {
            return mapper.readTree(dataRecord);
        } catch (Exception e) {
            ObjectNode errorDataRecord = new ObjectNode(new JsonNodeFactory(false));
            errorDataRecord.put("error", dataRecord);

            ObjectNode errorDataSource = new ObjectNode(new JsonNodeFactory(false));
            errorDataSource.put("error", errorDataRecord);

            return errorDataSource;
        }
    }

    private void parseNode(Map<String, DataRecord> dataRecords, JsonNode node) {
        if (node.isArray()) {
            parseArray(dataRecords, node);
        } else if (node.isObject()) {
            parseObject(dataRecords, node);
        }
    }

    private void parseArray(Map<String, DataRecord> dataRecords, JsonNode array) {
        Iterator<JsonNode> elements = array.elements();
        while (elements.hasNext()) {
            JsonNode node = elements.next();
            parseNode(dataRecords, node);
        }
    }

    private void parseObject(Map<String, DataRecord> dataRecords, JsonNode object) {
        Iterator<String> dataSource = object.fieldNames();
        if (dataSource.hasNext()) {
            String dataSourceName = dataSource.next();
            Iterator<Map.Entry<String, JsonNode>> records = object.get(dataSourceName).fields();
            if (records.hasNext()) {
                Map<String, Object> recordMap = newLinkedHashMap();
                while (records.hasNext()) {
                    Map.Entry<String, JsonNode> record = records.next();
                    String key = record.getKey();
                    Object value = record.getValue().asText();
                    recordMap.put(key, value);
                }
                dataRecords.put(dataSourceName, BasicDataRecord.fromMap(recordMap));
            }
        }
    }
}
