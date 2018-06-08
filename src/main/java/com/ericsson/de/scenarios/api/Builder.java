package com.ericsson.de.scenarios.api;

/**
 * Generic interface that classes use to implement their specific `build` implementation.
 */
public interface Builder<T> {

    T build();

}
