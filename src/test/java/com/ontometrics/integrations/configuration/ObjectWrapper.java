package com.ontometrics.integrations.configuration;

/**
 * Object wrapper
 * ObjectWrapper.java
 */
public class ObjectWrapper<T> {
    private T object;

    public T get() {
        return object;
    }

    public void set(T object) {
        this.object = object;
    }
}
