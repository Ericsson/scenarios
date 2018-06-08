package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.assertj.core.data.Index;
import org.junit.Test;

public class ConverterTest {

    @Test
    public void verifyTransformTypeConversion() {
        final String csvStringArrayMock = "2,3,1";

        DefaultDataRecordTransformer transformer = new DefaultDataRecordTransformer();
        final Object test_data = transformer.convert("Test Data", csvStringArrayMock, String[].class);
        assertThat(test_data).isExactlyInstanceOf(String[].class);
        final String[] numberArray = (String[]) test_data;
        assertThat(numberArray).contains("2", Index.atIndex(0));
        assertThat(numberArray).contains("3", Index.atIndex(1));
        assertThat(numberArray).contains("1", Index.atIndex(2));
    }
}
