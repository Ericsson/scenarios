/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.de.scenarios.impl.graph.jgrapht;

public abstract class AttributeProvider<T> {
    final private String name;

    public AttributeProvider(String name) {
        this.name = name;
    }

    String getAttributeId() {
        return name.replace(" ", "_").toLowerCase();
    }

    String getAttributeName() {
        return name;
    }

    public abstract String getAttributeValue(T t);
}
