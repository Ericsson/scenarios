package com.ericsson.de.scenarios.impl;

import static java.lang.String.format;

import static jodd.typeconverter.TypeConverterManager.convertType;

import java.beans.Introspector;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordTransformer;
import com.google.common.base.Preconditions;

/**
 * Creates Java Bean of given type from {@link DataRecord}.
 * Proxies call of getters of given class to access values of {@link DataRecord}
 *
 * @param <T>
 */
public class DefaultDataRecordTransformer<T> implements DataRecordTransformer<T> {

    @Override
    public T transform(final DataRecord dataRecord, Class<T> type) {
        if (type == DataRecord.class) {
            return type.cast(dataRecord);
        }

        return type.cast(Proxy.newProxyInstance(DataRecord.class.getClassLoader(), new Class[] { type }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass().equals(DataRecord.class) || method.getDeclaringClass().equals(Object.class)) {
                    return method.invoke(dataRecord, args);
                }

                Preconditions.checkState(isGetter(method, args), "Only getters are supported in DataRecord interface! Got: " + method);
                if ("getAllFields".equalsIgnoreCase(method.getName())) {
                    return dataRecord.getAllFields();
                }

                String key = Introspector.decapitalize(method.getName().substring(3));
                Object value = dataRecord.getFieldValue(key);
                return convert(key, value, method.getReturnType());
            }

            private boolean isGetter(final Method method, final Object[] args) {
                return method.getReturnType().equals(Void.TYPE) || (!method.getName().startsWith("get") || (!method.getName().startsWith("is"))) || (
                        args != null && args.length != 0);
            }
        }));
    }

    public <U> U convert(String name, Object value, Class<U> targetType) {
        if (value == null) {
            return null;
        } else if (targetType.isInstance(value)) {
            return targetType.cast(value);
        } else if (value instanceof DataRecord && targetType.getInterfaces()[0] == DataRecord.class) {
            return (U) transform(DataRecord.class.cast(value), (Class<T>) targetType);
        } else {
            try {
                return convertType(value, targetType);
            } catch (Exception e) {
                throw new IllegalArgumentException(format("Unable to convert field `%s` of class `%s` to `%s", name, value.getClass().getSimpleName(),
                        targetType.getSimpleName()));
            }
        }
    }

    @Override
    public boolean canTransformTo(Class type) {
        return type.isInterface();
    }
}
