package com.ericsson.de.scenarios.api;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class BasicDataRecordTest {
    @Test
    public void copy() throws Exception {
        DataRecord immutable = BasicDataRecord.fromValues("one", "immutable", "two", "value");

        DataRecord copy = BasicDataRecord.copy(immutable).setField("one", "new").build();

        assertThat(immutable.getFieldValue("one")).isEqualTo("immutable");
        assertThat(copy.getFieldValue("one")).isEqualTo("new");
        assertThat(copy.getFieldValue("two")).isEqualTo("value");
    }
}
