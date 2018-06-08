package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.impl.Internals.Exec;
import com.ericsson.de.scenarios.impl.Internals.VUser;
import com.google.common.collect.Maps;

public class InternalsTest extends ScenarioTest {

    @Test
    public void exec_verifyRootExec() throws Exception {
        Exec exec = Exec.rootExec(Maps.<String, Object>newHashMap());

        assertThat(exec.flowPath).isEmpty();
        assertThat(exec.vUser).isSameAs(VUser.ROOT);
        assertThat(exec.parent).isNull();
        assertThat(exec.dataRecord).isNull();
    }

    @Test
    public void exec_verifyChildrenCreation() throws Exception {
        DataRecordWrapper dataRecord1 = getDataRecords("ds_name", "1");
        DataRecordWrapper dataRecord2 = getDataRecords("ds_name", "2");

        Exec parent = Exec.rootExec(Maps.<String, Object>newHashMap());
        Exec child1 = parent.child("flow1", 1, dataRecord1);
        Exec child2 = parent.child("flow2", 2, dataRecord2);

        assertThat(child1.flowPath).isEqualTo("flow1");
        assertThat(child1.vUser.getId()).isEqualTo("1");
        assertThat(child1.parent).isSameAs(parent);
        assertThat(child1.dataRecord).isSameAs(dataRecord1);

        assertThat(child2.flowPath).isEqualTo("flow2");
        assertThat(child2.vUser.getId()).isEqualTo("2");
        assertThat(child2.parent).isSameAs(parent);
        assertThat(child2.dataRecord).isSameAs(dataRecord2);
    }

    @Test
    public void vUser_verifyIdComposition() throws Exception {
        VUser parent = VUser.ROOT;
        VUser child = parent.child(1);
        VUser grandChild1 = child.child(1);
        VUser grandChild2 = child.child(2);

        assertThat(parent.getId()).isEqualTo("");
        assertThat(child.getId()).isEqualTo("1");
        assertThat(grandChild1.getId()).isEqualTo("1.1");
        assertThat(grandChild2.getId()).isEqualTo("1.2");
    }

    @Test
    public void vUser_rootChildren_shouldHaveScenarioLevel() throws Exception {
        VUser parent = VUser.ROOT;
        VUser child1 = parent.child(1);
        VUser child2 = parent.child(2);
        VUser grandChild1 = child1.child(1);
        VUser grandChild2 = child2.child(2);

        assertThat(parent.isScenarioLevel()).isFalse();
        assertThat(child1.isScenarioLevel()).isTrue();
        assertThat(child2.isScenarioLevel()).isTrue();
        assertThat(grandChild1.isScenarioLevel()).isFalse();
        assertThat(grandChild2.isScenarioLevel()).isFalse();
    }

    @Test
    public void vUser_verifyEqualityById() throws Exception {
        VUser vUser1 = VUser.ROOT.child(1);
        VUser vUser2 = VUser.ROOT.child(1);

        assertThat(vUser1).isEqualTo(vUser2);
        assertThat(vUser1).isNotSameAs(vUser2);
        assertThat(vUser1.getId()).isEqualTo(vUser2.getId());
    }
}
