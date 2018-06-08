package com.ericsson.de.scenarios.api;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.api.RxApiImpl.flow;
import static com.ericsson.de.scenarios.api.RxApiImpl.scenario;
import static com.ericsson.de.scenarios.impl.RxApi.fromIterable;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ericsson.de.scenarios.Node;
import com.ericsson.de.scenarios.impl.ScenarioTest;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

public class DataSourceDefinitionTest {

    private List<Integer> iterable = newArrayList(1, 2, 3);

    @Test
    public void shouldSupportBuilders() throws Exception {
        scenario().addFlow(flow().addTestStep(ScenarioTest.nop()).withDataSources(fromIterable("dataSource", iterable).shared())).build();
    }

    @Test
    public void shouldSupportReusability() throws Exception {
        DataSource<Integer> dataSource = fromIterable("dataSource", iterable);

        scenario().addFlow(getReusableFlow().withDataSources(dataSource)).build();
    }

    private FlowBuilderInterfaces.Steps<Flow> getReusableFlow() {
        return flow("1").addTestStep(ScenarioTest.nop());
    }

    @Test
    public void shouldSupportDefinitionMultipleTimes() throws Exception {
        DataSource<Integer> sharedDataSource = fromIterable("dataSource", iterable).shared();

        scenario().addFlow(flow("1").addTestStep(ScenarioTest.nop()).withVUsers(3).withDataSources(sharedDataSource))
                .addFlow(flow("2").addTestStep(ScenarioTest.nop()).withVUsers(3).withDataSources(sharedDataSource)).build();
    }

    @Test
    public void couldBeUsedDifferently() throws Exception {
        final int vUsers = 3;
        ScenarioTest.Counter normalCounter = new ScenarioTest.Counter();
        ScenarioTest.Counter sharedCounter = new ScenarioTest.Counter();

        DataSource<Integer> dataSource = fromIterable("dataSource", iterable);

        Scenario scenario = scenario().addFlow(flow("1").addTestStep(sharedCounter).withVUsers(vUsers).withDataSources(dataSource.shared()))
                .addFlow(flow("2").addTestStep(normalCounter).withVUsers(vUsers).withDataSources(dataSource)).build();

        RxApiImpl.run(scenario);

        normalCounter.assertEqualTo(iterable.size() * vUsers);
        sharedCounter.assertEqualTo(iterable.size());
    }

    @Test
    public void copyOnConfigurationDoesNotAffectState() throws Exception {
        DataSource<Integer> dataSource = fromIterable("dataSource", iterable);

        DataSource<Integer> sharedDataSource = dataSource.shared();
        DataSource<Integer> filteredDataSource = dataSource.filterField("dataSource").equalTo(1);

        DataSource<Integer> sharedFiltered1 = sharedDataSource.filterField("dataSource").equalTo(1);
        DataSource<Integer> sharedFiltered2 = filteredDataSource.shared();

        assertThat(sharedDataSource.isShared()).isTrue();
        assertThat(sharedDataSource.filters).hasSize(0);

        assertThat(filteredDataSource.isShared()).isFalse();
        assertThat(filteredDataSource.filters).hasSize(1);

        assertThat(sharedFiltered1.isShared()).isTrue();
        assertThat(sharedFiltered1.filters).hasSize(1);

        assertThat(sharedFiltered2.isShared()).isTrue();
        assertThat(sharedFiltered2.filters).hasSize(1);
    }

    @Test
    public void toString_shouldContainMetadata() throws Exception {
        DataSource<Integer> plain = fromIterable("numbers", iterable);
        DataSource<Integer> shared = plain.shared();
        DataSource<Integer> cyclic = plain.cyclic();
        DataSource<Integer> sharedCyclic = plain.shared().cyclic();

        assertThat(plain.toString()).isEqualTo("Data Source 'numbers'");
        assertThat(shared.toString()).isEqualTo("shared Data Source 'numbers'");
        assertThat(cyclic.toString()).isEqualTo("cyclic Data Source 'numbers'");
        assertThat(sharedCyclic.toString()).isEqualTo("shared, cyclic Data Source 'numbers'");
    }

    @Test
    public void iterableType() throws Exception {
        ArrayList<Node> nodes = newArrayList(
                BasicDataRecord.builder().setField("networkElementId", "SGSN-14B").setField("nodeType", "LTE01ERB").build(Node.class));

        DataSource<Node> nodesDataSource = fromIterable("nodes", nodes);
        DataSource<String> stringsDataSource = fromIterable("strings", newArrayList("one", "two"));

        assertThat(nodesDataSource.getType()).isEqualTo(Node.class);
        assertThat(stringsDataSource.getType()).isEqualTo(String.class);
    }
}
