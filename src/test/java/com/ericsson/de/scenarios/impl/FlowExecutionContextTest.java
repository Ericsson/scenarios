package com.ericsson.de.scenarios.impl;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import static org.assertj.core.api.Assertions.assertThat;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.ListAssert;
import org.junit.Test;

import com.ericsson.de.scenarios.api.Api;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.Flow;
import com.ericsson.de.scenarios.impl.FlowExecutionContext.DataRecordsToExecutions;
import com.ericsson.de.scenarios.impl.Internals.Exec;
import com.ericsson.de.scenarios.impl.Internals.InternalScenarioContext;
import com.ericsson.de.scenarios.impl.Internals.VUser;
import com.google.common.collect.Maps;

public class FlowExecutionContextTest {

    @Test
    public void toExecutions_regular() throws Exception {
        List<Exec> parents = executions("a", "b", "c");
        List<DataRecordWrapper> dataRecords = dataRecords("q", "w", "e", "r", "t", "y");

        List<Exec> newExecs = toExecutions(2, parents, 0, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("1.1", "1.2", "2.1", "2.2", "3.1", "3.2");
    }

    @Test
    public void toExecutions_last() throws Exception {
        List<Exec> parents = executions("a", "b", "c");
        List<DataRecordWrapper> dataRecords = dataRecords("q", "w", "e");

        List<Exec> newExecs = toExecutions(2, parents, 0, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("1.1", "1.2", "2.1");
    }

    @Test
    public void toExecutions_first() throws Exception {
        List<Exec> parents = rootExecutions();
        List<DataRecordWrapper> dataRecords = dataRecords("q", "w", "e");

        List<Exec> newExecs = toExecutions(3, parents, 0, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("1", "2", "3");
    }

    @Test
    public void toExecutions_shift() throws Exception {
        List<Exec> parents = rootExecutions();
        List<DataRecordWrapper> dataRecords = dataRecords("q", "w", "e");

        List<Exec> newExecs = toExecutions(3, parents, 10, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("11", "12", "13");
    }

    private List<Exec> executions(String... values) {
        List<Exec> executions = newArrayListWithCapacity(values.length);
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            executions.add(exec(i + 1, value));
        }
        return executions;
    }

    private List<Exec> rootExecutions() {
        Map<String, Object> parameters = emptyMap();
        Exec exec = Exec.rootExec(parameters);
        return singletonList(exec);
    }

    private Exec exec(int number, String value) {
        VUser vUser = VUser.ROOT.child(number);
        DataRecordWrapper dataRecord = ScenarioTest.getDataRecords("ds_name", value);
        InternalScenarioContext context = new InternalScenarioContext(Maps.<String, Object>newHashMap());
        return new Exec("rxFlow", vUser, context, null, dataRecord);
    }

    private List<DataRecordWrapper> dataRecords(String... values) {
        List<DataRecordWrapper> dataRecords = newArrayListWithCapacity(values.length);
        for (String value : values) {
            dataRecords.add(ScenarioTest.getDataRecords("ds_name", value));
        }
        return dataRecords;
    }

    private List<Exec> toExecutions(int vUsers, List<Exec> parents, int vUserOffset, List<DataRecordWrapper> dataRecords) {
        Flow flow = Api.flow("rxFlow").addTestStep(ScenarioTest.nop()).withVUsers(vUsers).build();
        return new DataRecordsToExecutions(flow, parents, vUserOffset).call(dataRecords);
    }

    private ListAssert<Object> assertThatExecutionVUserIds(List<Exec> newExecs) {
        return assertThat(newExecs).extracting("vUser.id");
    }
}
