package com.ericsson.de.scenarios.impl;

import com.ericsson.de.scenarios.api.Identifiable;
import com.ericsson.de.scenarios.api.TestStep;

/**
 * Could be {@link TestStep} or {@link Internals.Fork}
 */
public abstract class Invocation implements Identifiable<Long> {
    Long id = null;

    @Override
    public Long getId() {
        return id;
    }

    public abstract Invocation alwaysRun();
}
