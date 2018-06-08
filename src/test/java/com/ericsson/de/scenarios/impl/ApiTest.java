package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.RxApi.fromDataRecords;
import static com.ericsson.de.scenarios.impl.RxApi.fromIterable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.DataSource;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import rx.Observable;

public class ApiTest {

    @Test
    public void testFromIterable() throws Exception {
        final String ds_name = "ds_";
        String firstValue = "one";

        List<Object> iterable = Arrays.<Object>asList(firstValue, "two", "three");

        DataSource provider = fromIterable(ds_name, iterable);

        DataSourceStrategy strategy = DataSourceStrategy.fromDefinition(provider, 1);

        Observable<DataRecordWrapper> observable = strategy.provide();

        assertThat(observable.count().toBlocking().first()).isEqualTo(3);

        DataRecordWrapper firstDataRecords = observable.toBlocking().first();
        Optional<String> optionalValue = firstDataRecords.getFieldValue(ds_name, String.class);
        assertThat(optionalValue.isPresent()).isTrue();
        assertThat(optionalValue.get()).isEqualTo(firstValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failOnEmptyIterable() throws Exception {
        fromIterable("empty", new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failOnEmptyDataRecordsArray() throws Exception {
        fromDataRecords("empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void inconsistentIterable() throws Exception {
        Iterable<Integer> inconsistentIterable = new Iterable<Integer>() {
            List<Integer> values = Lists.newArrayList(1, 2);

            @Override
            public Iterator<Integer> iterator() {
                values.add(values.size());
                return values.iterator();
            }
        };

        fromIterable("crazy", inconsistentIterable);
    }
}
