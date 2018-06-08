package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ericsson.de.scenarios.Node;
import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.google.common.collect.ImmutableMap;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

public class DataRecordsTest extends ScenarioTest {

    private final String DS_NAME_1 = "dsName1";
    private final String DS_NAME_2 = "dsName2";

    private final String DR_NAME1 = "networkElementId";
    private final String DR1_VALUE1 = "SGSN-14B";
    private final String DR1_VALUE2 = "SGSN-MME";

    private final String DR_NAME2 = "nodeType";
    private final String DR2_VALUE1 = "LTE01ERB";
    private final String DR2_VALUE2 = "ERBS";

    private final String ORIGINAL_VALUE_1 = DR1_VALUE1;
    private final String ORIGINAL_VALUE_2 = DR1_VALUE2;
    private final String OVERRIDEN_VALUE_1 = DR2_VALUE1;
    private final String OVERRIDEN_VALUE_2 = DR2_VALUE2;

    private DataRecord testDataRecord1 = BasicDataRecord.fromValues(DR_NAME1, DR1_VALUE1, DR_NAME2, DR1_VALUE2);
    private DataRecord testDataRecord2 = BasicDataRecord.fromValues(DR_NAME1, DR2_VALUE1, DR_NAME2, DR2_VALUE2);

    @Test
    public void testSingle() throws Exception {
        DataRecordWrapper dataRecords = getDataRecords(DS_NAME_1, testDataRecord1);

        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(dataRecords, DR_NAME2)).isEqualTo(DR1_VALUE2);

        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME2)).isEqualTo(DR1_VALUE2);
        assertThat(dataRecords.getFieldValue(DS_NAME_2 + "." + DR_NAME2, String.class).isPresent()).isFalse();

        assertThat(dataRecords.getFieldValue(DS_NAME_1, Integer.class).isPresent()).isFalse();

        DataRecord toDataRecord = input(dataRecords, DS_NAME_1, DataRecord.class);
        assertThat(toDataRecord.getFieldValue(DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(toDataRecord.getFieldValue(DR_NAME2)).isEqualTo(DR1_VALUE2);

        Node toDataRecord2 = dataRecords.getFieldValue(DS_NAME_1, Node.class).get();
        assertThat(toDataRecord2.getNetworkElementId()).isEqualTo(DR1_VALUE1);
        assertThat(toDataRecord2.getNodeType()).isEqualTo(DR1_VALUE2);
    }

    @Test
    public void testSingleSerialization() throws Exception {
        DataRecordWrapper dataRecords = getDataRecords(DS_NAME_1, testDataRecord1);
        Map<String, Object> map = fromJson(dataRecords.toString());

        assertThat(map).containsOnlyKeys(DS_NAME_1);
        assertThat((Map) map.get(DS_NAME_1)).containsOnlyKeys(DR_NAME1, DR_NAME2);
    }

    @Test
    public void testMultiple() throws Exception {
        DataRecordWrapper first = getDataRecords(DS_NAME_1, testDataRecord1);
        DataRecordWrapper second = getDataRecords(DS_NAME_2, testDataRecord2);

        DataRecordWrapper dataRecords = new DataRecords.Multiple(first, second);

        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(DR1_VALUE1); //resolution without full path

        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(dataRecords, DS_NAME_2 + "." + DR_NAME1)).isEqualTo(DR2_VALUE1);

        Node toDataRecord1 = input(dataRecords, DS_NAME_1, Node.class);
        assertThat(toDataRecord1.getNetworkElementId()).isEqualTo(DR1_VALUE1);

        Node toDataRecord2 = input(dataRecords, DS_NAME_2, Node.class);
        assertThat(toDataRecord2.getNetworkElementId()).isEqualTo(DR2_VALUE1);
    }

    @Test
    public void testMultipleSerialization() throws Exception {
        DataRecordWrapper first = getDataRecords(DS_NAME_1, testDataRecord1);
        DataRecordWrapper second = getDataRecords(DS_NAME_2, testDataRecord2);

        DataRecordWrapper dataRecords = new DataRecords.Multiple(first, second);

        List<Map<String, Object>> list = fromJson(dataRecords.toString());

        assertThat(list).hasSize(2);

        assertThat((Map) list.get(0)).containsOnlyKeys(DS_NAME_1);
        assertThat((Map) list.get(0).get(DS_NAME_1)).containsEntry(DR_NAME1, DR1_VALUE1);
        assertThat((Map) list.get(0).get(DS_NAME_1)).containsEntry(DR_NAME2, DR1_VALUE2);

        assertThat((Map) list.get(1)).containsOnlyKeys(DS_NAME_2);
        assertThat((Map) list.get(1).get(DS_NAME_2)).containsEntry(DR_NAME1, DR2_VALUE1);
        assertThat((Map) list.get(1).get(DS_NAME_2)).containsEntry(DR_NAME2, DR2_VALUE2);
    }

    @Test
    public void testParentChild() throws Exception {
        DataRecordWrapper child = getDataRecords(DS_NAME_1, testDataRecord1);
        DataRecordWrapper parent = getDataRecords(DS_NAME_2, testDataRecord2);

        DataRecordWrapper dataRecords = new DataRecords.Parent(child, parent);

        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(DR1_VALUE1); //resolution without full path

        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(dataRecords, DS_NAME_2 + "." + DR_NAME1)).isEqualTo(DR2_VALUE1);

        Node toDataRecord1 = input(dataRecords, DS_NAME_1, Node.class);
        assertThat(toDataRecord1.getNetworkElementId()).isEqualTo(DR1_VALUE1);

        Node toDataRecord2 = input(dataRecords, DS_NAME_2, Node.class);
        assertThat(toDataRecord2.getNetworkElementId()).isEqualTo(DR2_VALUE1);
    }

    @Test
    public void testParentChildSerialization() throws Exception {
        DataRecordWrapper parent = getDataRecords(DS_NAME_1, testDataRecord1);
        DataRecordWrapper child = getDataRecords(DS_NAME_2, testDataRecord2);

        DataRecordWrapper dataRecords = new DataRecords.Parent(parent, child);

        List<Map<String, Object>> list = fromJson(dataRecords.toString());

        assertThat(list).hasSize(2);

        assertThat((Map) list.get(0)).containsOnlyKeys(DS_NAME_1);
        assertThat((Map) list.get(0).get(DS_NAME_1)).containsEntry(DR_NAME1, DR1_VALUE1);
        assertThat((Map) list.get(0).get(DS_NAME_1)).containsEntry(DR_NAME2, DR1_VALUE2);

        assertThat((Map) list.get(1)).containsOnlyKeys(DS_NAME_2);
        assertThat((Map) list.get(1).get(DS_NAME_2)).containsEntry(DR_NAME1, DR2_VALUE1);
        assertThat((Map) list.get(1).get(DS_NAME_2)).containsEntry(DR_NAME2, DR2_VALUE2);
    }

    @Test
    public void testParameter() throws Exception {
        DataRecordWrapper dataRecord = getDataRecords(DS_NAME_1, testDataRecord1);
        Map<String, Object> parameters = ImmutableMap.of(DR_NAME1, (Object) OVERRIDEN_VALUE_1, DR_NAME2, OVERRIDEN_VALUE_2);

        DataRecordWrapper dataRecords = new DataRecords.Parameter(dataRecord, parameters);

        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(OVERRIDEN_VALUE_1);
        assertThat(input(dataRecords, DR_NAME2)).isEqualTo(OVERRIDEN_VALUE_2);

        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME1)).isEqualTo(ORIGINAL_VALUE_1);
        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME2)).isEqualTo(ORIGINAL_VALUE_2);

        Node toDataRecord1 = input(dataRecords, DS_NAME_1, Node.class);
        assertThat(toDataRecord1.getNetworkElementId()).isEqualTo(ORIGINAL_VALUE_1);
        assertThat(toDataRecord1.getNodeType()).isEqualTo(ORIGINAL_VALUE_2);
    }

    @Test
    public void testParameter_fullName() throws Exception {
        DataRecordWrapper dataRecord = getDataRecords(DS_NAME_1, testDataRecord1);
        Map<String, Object> parameters = ImmutableMap
                .of(DS_NAME_1 + "." + DR_NAME1, (Object) OVERRIDEN_VALUE_1, DS_NAME_1 + "." + DR_NAME2, (Object) OVERRIDEN_VALUE_2);

        DataRecordWrapper dataRecords = new DataRecords.Parameter(dataRecord, parameters);

        assertThat(input(dataRecords, DR_NAME1)).isEqualTo(OVERRIDEN_VALUE_1);
        assertThat(input(dataRecords, DR_NAME2)).isEqualTo(OVERRIDEN_VALUE_2);

        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME1)).isEqualTo(OVERRIDEN_VALUE_1);
        assertThat(input(dataRecords, DS_NAME_1 + "." + DR_NAME2)).isEqualTo(OVERRIDEN_VALUE_2);

        Node toDataRecord1 = input(dataRecords, DS_NAME_1, Node.class);
        assertThat(toDataRecord1.getNetworkElementId()).isEqualTo(OVERRIDEN_VALUE_1);
        assertThat(toDataRecord1.getNodeType()).isEqualTo(OVERRIDEN_VALUE_2);
    }

    @Test
    public void testParameterSerialization() throws Exception {
        DataRecordWrapper dataRecord = getDataRecords(DS_NAME_1, testDataRecord1);
        Map<String, Object> parameters = ImmutableMap
                .of(DS_NAME_1 + "." + DR_NAME1, (Object) OVERRIDEN_VALUE_1, DS_NAME_1 + "." + DR_NAME2, (Object) OVERRIDEN_VALUE_2);

        DataRecordWrapper dataRecords = new DataRecords.Parameter(dataRecord, parameters);

        List<Map<String, Object>> list = fromJson(dataRecords.toString());

        assertThat(list).hasSize(2);

        assertThat((Map) list.get(0)).containsOnlyKeys(DS_NAME_1);
        assertThat((Map) list.get(0).get(DS_NAME_1)).containsEntry(DR_NAME1, OVERRIDEN_VALUE_1);
        assertThat((Map) list.get(0).get(DS_NAME_1)).containsEntry(DR_NAME2, OVERRIDEN_VALUE_2);

        assertThat((Map) list.get(1)).containsOnlyKeys(DS_NAME_1);
        assertThat((Map) list.get(1).get(DS_NAME_1)).containsEntry(DR_NAME1, ORIGINAL_VALUE_1);
        assertThat((Map) list.get(1).get(DS_NAME_1)).containsEntry(DR_NAME2, ORIGINAL_VALUE_2);
    }

    @Test
    public void testOnion() throws Exception {
        final String dr3_name1 = "DR3_NAME1";
        final String dr3_value1 = "DR3_VALUE1";

        DataRecord testDataRecord3 = BasicDataRecord.fromValues(dr3_name1, dr3_value1);

        DataRecordWrapper child1 = getDataRecords(DS_NAME_1, testDataRecord1);
        DataRecordWrapper child2 = getDataRecords(DS_NAME_2, testDataRecord2);
        DataRecordWrapper parent = getDataRecords("DS_NAME_3", testDataRecord3);

        DataRecords.Parent onion = new DataRecords.Parent(new DataRecords.Multiple(child1, child2),
                new DataRecords.Parent(parent, new DataRecords.Empty("scenario")));

        assertThat(input(onion, DR_NAME1)).isEqualTo(DR1_VALUE1);
        assertThat(input(onion, DS_NAME_2 + "." + DR_NAME1)).isEqualTo(DR2_VALUE1);
        assertThat(input(onion, dr3_name1)).isEqualTo(dr3_value1);

        assertThat(onion.getIteration()).isEqualTo("0.0.0-0");
    }

    @Test
    public void testNestedDataRecords() throws Exception {
        DataRecord testDataRecord3 = BasicDataRecord.fromValues("level3", DR1_VALUE1);
        DataRecord testDataRecord2 = BasicDataRecord.fromValues("level2", testDataRecord3);
        DataRecord testDataRecord1 = BasicDataRecord.fromValues("level1", testDataRecord2);

        DataRecordWrapper wrapper2 = getDataRecords(DS_NAME_1, testDataRecord2);
        DataRecordWrapper wrapper1 = getDataRecords(DS_NAME_1, testDataRecord1);

        assertThat(input(wrapper2, "level2.level3")).isEqualTo(DR1_VALUE1);
        assertThat(input(wrapper1, "level1.level2.level3")).isEqualTo(DR1_VALUE1);
    }

    @Test
    public void testSimpleStringConversions() throws Exception {
        DataRecord fromCsv = BasicDataRecord.builder().setField("string", "value").setField("integer", "1").setField("boolean", "true")
                .setField("double", "1.2").build();

        DataRecordWrapper wrapper = getDataRecords(DS_NAME_1, fromCsv);

        assertThat(input(wrapper, "string")).isEqualTo("value");
        assertThat(input(wrapper, "integer")).isEqualTo("1");
        assertThat(input(wrapper, "double")).isEqualTo("1.2");
        assertThat(input(wrapper, "boolean")).isEqualTo("true");

        assertThat(input(wrapper, "integer", Integer.class)).isEqualTo(1);
        assertThat(input(wrapper, "integer", Double.class)).isEqualTo(1.0);
        assertThat(input(wrapper, "double", Double.class)).isEqualTo(1.2);
        assertThat(input(wrapper, "boolean", Boolean.class)).isTrue();
    }

    @Test
    public void testSimpleObjectsConversions() throws Exception {
        DataRecord fromCsv = BasicDataRecord.builder().setField("integer", 1).setField("double", 1.2).setField("long", 1L).build();

        DataRecordWrapper wrapper = getDataRecords(DS_NAME_1, fromCsv);

        assertThat(input(wrapper, "integer", String.class)).isEqualTo("1");
        assertThat(input(wrapper, "double", String.class)).isEqualTo("1.2");

        assertThat(input(wrapper, "integer", Integer.class)).isEqualTo(1);
        assertThat(input(wrapper, "long", Integer.class)).isEqualTo(1);

        assertThat(input(wrapper, "long", Long.class)).isEqualTo(1L);
        assertThat(input(wrapper, "integer", Long.class)).isEqualTo(1L);

        assertThat(input(wrapper, "integer", Double.class)).isEqualTo(1.0);
        assertThat(input(wrapper, "long", Double.class)).isEqualTo(1.0);
        assertThat(input(wrapper, "double", Double.class)).isEqualTo(1.2);
    }
}
