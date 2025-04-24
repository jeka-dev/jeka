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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class to deal with time.
 * 
 * @author Jerome Angibaud
 */
public final class JkUtilsTime {

    private JkUtilsTime() {}

    /**
     * Returns the current ofSystem date
     */
    public static Date now() {
        return new Date();
    }

    /**
     * Returns the current ofSystem date as string with the specified format
     */
    public static String now(String pattern) {
        return new SimpleDateFormat(pattern).format(now());
    }

    /**
     * Returns the current ofSystem date as string with the specified format
     */
    public static String nowUtc(String pattern) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(now());
    }

    /**
     * Returns the duration in second between the specified nano time and now.
     */
    public static float durationInSeconds(long startNano) {
        final long endNano = System.nanoTime();
        final long durationMillis = (endNano - startNano) / 1000000;
        return durationMillis / 1000f;
    }

    /**
     * Returns the duration in millisecond between the specified nano time and
     * now.
     */
    public static long durationInMillis(long startNano) {
        final long endNano = System.nanoTime();
        return (endNano - startNano) / 1000000;
    }

    /**
     * Formats the given duration in milliseconds into a human-readable string.
     * The format of the string is combination of hours, minutes, seconds, and milliseconds.
     *
     * @param durationInMillis The duration in milliseconds.
     * @return The formatted string representing the duration.
     */
    public static String formatMillis(long durationInMillis) {
        Duration duration = Duration.ofMillis(durationInMillis);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
        long millis = duration.minusHours(hours).minusMinutes(minutes).minusSeconds(seconds).toMillis();
        String result = "";
        if (hours > 0) {
            result += hours + "h";
        }
        if (minutes > 0) {
            result += minutes + "m";
        }
        if (seconds > 0) {
            result += seconds + "s";
        }
        result += millis + "ms";
        return result;
    }
}
