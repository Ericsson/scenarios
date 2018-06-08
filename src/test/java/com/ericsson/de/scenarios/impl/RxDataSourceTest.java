package com.ericsson.de.scenarios.impl;

import static com.ericsson.de.scenarios.impl.RxDataSource.copy;
import static com.ericsson.de.scenarios.impl.RxDataSource.multiply;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.junit.Test;

import com.ericsson.de.scenarios.RxJavaExploratoryTest;
import com.ericsson.de.scenarios.api.DataRecordWrapper;

import rx.Observable;

public class RxDataSourceTest {

    @Test
    public void flowSubflowDataSources() throws Exception {
        Observable<DataRecordWrapper> flowDataSource = newDataSource("1", "2");
        Observable<DataRecordWrapper> subFlowDataSource = newDataSource("a", "b", "c");

        multiply(flowDataSource, subFlowDataSource).subscribe(RxJavaExploratoryTest.PrinterAction.INSTANCE);
    }

    @Test
    public void notSharedDataSource() throws Exception {
        int vUsers = 3;
        Observable<DataRecordWrapper> notSharedDataSource = newDataSource("a", "b", "c");

        copy(notSharedDataSource, vUsers).buffer(vUsers).subscribe(RxJavaExploratoryTest.PrinterAction.INSTANCE);
    }

    @Test
    public void copyTest() throws Exception {
        int vUsers = 8;
        Observable<DataRecordWrapper> notSharedDataSource = newDataSource("1-a-x", "1-a-o", "1-a-x", "1-a-o", "1-a-x", "1-a-o", "1-a-x", "1-a-o");

        copy(notSharedDataSource, 2).buffer(vUsers).subscribe(RxJavaExploratoryTest.PrinterAction.INSTANCE);
    }

    private static Observable<DataRecordWrapper> newDataSource(String... strings) {
        List<DataRecordWrapper> dataRecords = newArrayList();

        for (String s : strings) {
            dataRecords.add(ScenarioTest.getDataRecords("ds_name", s));
        }

        return Observable.from(dataRecords);
    }
}
