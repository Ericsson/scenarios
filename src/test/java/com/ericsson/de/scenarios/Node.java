package com.ericsson.de.scenarios;

import com.ericsson.de.scenarios.api.DataRecord;

public interface Node extends DataRecord {
    String NODE_TYPE = "nodeType";
    String NETWORK_ELEMENT_ID = "networkElementId";
    String PORT = "port";

    String getNodeType();

    String getNetworkElementId();

    Integer getPort();
}
