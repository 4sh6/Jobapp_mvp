
package com.example;

import java.lang.reflect.Field;

public class TestUtils {

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> c = target.getClass();
            while (c != Object.class) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException ex) {
                    c = c.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
