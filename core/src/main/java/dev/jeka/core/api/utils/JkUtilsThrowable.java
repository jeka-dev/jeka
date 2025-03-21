/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.utils;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Utility class to deal with {@link Throwable}
 */
public final class JkUtilsThrowable {

    private JkUtilsThrowable() {
        // prevent instantiation
    }

    /**
     * Returns the specified exception itself if it is an unchecked exception
     * (extending {@link RuntimeException}). Otherwise, returns a new
     * {@link RuntimeException} wrapping the exception passed as argument.
     */
    public static RuntimeException unchecked(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    /**
     * Same as {@link #unchecked(Exception)} but specifying an error message.
     */
    public static RuntimeException unchecked(Throwable e, String message) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(message, e);
    }

    /**
     * Returns <code>true</code> if the specified throwable has the specified cause exception class
     * along the specified message in its chain.
     */
    public static boolean nestedContains(Throwable throwable, Class<Exception> exceptionClass,
            String message) {
        if (throwable.getClass().equals(exceptionClass) && message.equals(throwable.getMessage())) {
            return true;
        }
        if (throwable.getCause() == null) {
            return false;
        }
        return nestedContains(throwable.getCause(), exceptionClass, message);
    }

    /**
     * Returns <code>true</code> if the specified throwable has the specified cause exception class
     * in its chain.
     */
    public static boolean isInCause(Throwable throwable, Class<? extends Throwable> causeClass) {
        if (causeClass.isAssignableFrom(throwable.getClass())) {
            return true;
        }
        final Throwable cause = throwable.getCause();
        if (cause == null) {
            return false;
        }
        return isInCause(cause, causeClass);
    }

    public interface ThrowingConsumer<T, E extends Throwable> {
        void accept(T t) throws E;

        static <T, E extends Throwable> Consumer<T> unchecked(ThrowingConsumer<T, E> consumer) {
            return (t) -> {
                try {
                    consumer.accept(t);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    public static void printStackTrace(PrintStream printStream, StackTraceElement[] stack, int maxElement) {
        int count = 0;
        for (StackTraceElement element : stack) {
            count++;
            if (count > maxElement) {
                break;
            }
            printStream.print("    at ");
            printStream.println(element.toString());
        }
        if (count < stack.length) {
            printStream.println("    ...");
        }
    }

    public static void printStackTrace(PrintStream printStream, Throwable e, int maxElement) {
        printStream.println(e.getClass().getName() + ": " + e.getMessage());
        printStackTrace(printStream, e.getStackTrace(), maxElement);
        if (e.getCause() != null) {
            printStream.print("Caused by: ");
            printStackTrace(printStream, e.getCause(), maxElement);
        }
    }

}
