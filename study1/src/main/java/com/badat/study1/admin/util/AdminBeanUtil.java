package com.badat.study1.admin.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AdminBeanUtil {
    private AdminBeanUtil(){}

    public static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) throw new IllegalArgumentException("Status must not be null");
        return Enum.valueOf(enumClass, value.trim().toUpperCase());
    }

    public static void setBoolean(Object target, String... candidateNamesAndValueLast) {
        boolean value = Boolean.parseBoolean(candidateNamesAndValueLast[candidateNamesAndValueLast.length - 1]);
        for (int i = 0; i < candidateNamesAndValueLast.length - 1; i++) {
            if (trySetterBoolean(target, candidateNamesAndValueLast[i], value)) return;
            if (tryFieldBoolean(target, candidateNamesAndValueLast[i], value)) return;
        }
        // im lặng nếu không có field phù hợp
    }

    public static void setString(Object target, String name, String value) {
        if (trySetterString(target, name, value)) return;
        tryFieldString(target, name, value);
    }

    /* setters */
    private static boolean trySetterBoolean(Object t, String name, boolean v) {
        String m1 = "set" + capitalize(name);
        try {
            Method m = t.getClass().getMethod(m1, boolean.class);
            m.invoke(t, v); return true;
        } catch (Exception ignore) { return false; }
    }
    private static boolean trySetterString(Object t, String name, String v) {
        String m1 = "set" + capitalize(name);
        try {
            Method m = t.getClass().getMethod(m1, String.class);
            m.invoke(t, v); return true;
        } catch (Exception ignore) { return false; }
    }

    /* fields */
    private static boolean tryFieldBoolean(Object t, String name, boolean v) {
        try {
            Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setBoolean(t, v); return true;
        } catch (Exception ignore) { return false; }
    }
    private static boolean tryFieldString(Object t, String name, String v) {
        try {
            Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(t, v); return true;
        } catch (Exception ignore) { return false; }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
