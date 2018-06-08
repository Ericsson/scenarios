/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.RxApi.fromDataRecords;

import org.junit.Test;

import com.ericsson.de.scenarios.Node;
import com.ericsson.de.scenarios.api.DataSource;
import com.google.common.collect.Iterables;

public class DataSourceStrategyTest extends ScenarioTest {

    @Test(timeout = 10000L)
    public void cyclicDoesNotLoopForever() throws Exception {
        DataSource<Node> ds1 = fromDataRecords("ds1", getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3));

        DataSource<Node> loop = fromDataRecords("loop", getNode("1", "2", 3), getNode("1", "2", 3)).cyclic();

        DataSource<Node> loop2 = fromDataRecords("loop2", getNode("1", "2", 3), getNode("1", "2", 3)).cyclic();

        DataSource<Node> loop3 = fromDataRecords("loop3", getNode("1", "2", 3), getNode("1", "2", 3)).cyclic();

        DataSource<Node> shared = fromDataRecords("shared", getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3)).shared();

        DataSourceStrategy multiple = DataSourceStrategy.fromDefinitions(new DataSource[] { ds1, loop, loop2, shared, loop3

        }, 2);

        Integer totalIterationCount = multiple.provide().count().toBlocking().single();
        assertThat(totalIterationCount).isEqualTo(Iterables.size(shared));
    }

    @Test(timeout = 10000L)
    public void cyclicSharedCopied() throws Exception {
        DataSource<Node> copied = fromDataRecords("copied", getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3));

        DataSource<Node> cyclic = fromDataRecords("cyclic", getNode("1", "2", 3), getNode("1", "2", 3)).cyclic();

        DataSource<Node> shared = fromDataRecords("shared", getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3)).shared();

        DataSourceStrategy multiple = DataSourceStrategy.fromDefinitions(new DataSource[] { copied, cyclic, shared, }, 2);

        Integer totalIterationCount = multiple.provide().count().toBlocking().single();
        assertThat(totalIterationCount).isEqualTo(Iterables.size(shared));
    }

    @Test(timeout = 10000L)
    public void cyclicCopied() throws Exception {
        DataSource<Node> largerCopied = fromDataRecords("largerCopied", getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3),
                getNode("1", "2", 3), getNode("1", "2", 3));

        DataSource<Node> smallerCopied = fromDataRecords("copied2", getNode("1", "2", 3), getNode("1", "2", 3), getNode("1", "2", 3),
                getNode("1", "2", 3));

        DataSource<Node> cyclic = fromDataRecords("cyclic", getNode("1", "2", 3), getNode("1", "2", 3)).cyclic();

        final int vUsers = 2;
        DataSourceStrategy multiple = DataSourceStrategy.fromDefinitions(new DataSource[] { largerCopied, smallerCopied, cyclic, }, vUsers);

        Integer totalIterationCount = multiple.provide().count().toBlocking().single();
        assertThat(totalIterationCount).isEqualTo(Iterables.size(smallerCopied) * vUsers);
    }

}
