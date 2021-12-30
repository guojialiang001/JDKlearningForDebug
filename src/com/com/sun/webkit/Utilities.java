/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.sun.webkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.reflect.misc.MethodUtil;

public abstract class Utilities {

    private static Utilities instance;

    public static synchronized void setUtilities(Utilities util) {
        instance = util;
    }

    public static synchronized Utilities getUtilities() {
        return instance;
    }

    protected abstract Pasteboard createPasteboard();
    protected abstract PopupMenu createPopupMenu();
    protected abstract ContextMenu createContextMenu();

    private static final Set<String> asSet(String... items) {
        return new HashSet(Arrays.asList(items));
    }

    // Whitelist of Class methods to allow
    private static final Set<String> classMethodsWhitelist = asSet(
        "getCanonicalName",
        "getEnumConstants",
        "getFields",
        "getMethods",
        "getName",
        "getPackageName",
        "getSimpleName",
        "getSuperclass",
        "getTypeName",
        "getTypeParameters",
        "isAssignableFrom",
        "isArray",
        "isEnum",
        "isInstance",
        "isInterface",
        "isLocalClass",
        "isMemberClass",
        "isPrimitive",
        "isSynthetic",
        "toGenericString",
        "toString"
    );

    // Blacklist of classes to disallow
    private static final Set<String> classesBlacklist = asSet(
        "java.lang.ClassLoader",
        "java.lang.Module",
        "java.lang.Runtime",
        "java.lang.System"
    );

    // Blacklist of packages to disallow
    private static final List<String> packagesBlacklist = Arrays.asList(
        "java.lang.invoke",
        "java.lang.module",
        "java.lang.reflect",
        "java.security",
        "sun.misc"
    );

    private static Object fwkInvokeWithContext(final Method method,
                                               final Object instance,
                                               final Object[] args,
                                               AccessControlContext acc)
            throws Throwable {

        final Class<?> clazz = method.getDeclaringClass();
        if (clazz.equals(java.lang.Class.class)) {
            // check whitelist of allowable Class methods
            if (!classMethodsWhitelist.contains(method.getName())) {
                throw new UnsupportedOperationException("invocation not supported");
            }
        } else {
            // check blacklist of class names
            final String className = clazz.getName();
            if (classesBlacklist.contains(className)) {
                throw new UnsupportedOperationException("invocation not supported");
            }
            // check blacklist of packages
            packagesBlacklist.forEach(packageName -> {
                if (className.startsWith(packageName + ".")) {
                    throw new UnsupportedOperationException("invocation not supported");
                }
            });
        }

        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> MethodUtil.invoke(method, instance, args), acc);
        } catch (PrivilegedActionException ex) {
            Throwable cause = ex.getCause();
            if (cause == null)
                cause = ex;
            else if (cause instanceof InvocationTargetException
                && cause.getCause() != null)
                cause = cause.getCause();
            throw cause;
        }
    }
}
