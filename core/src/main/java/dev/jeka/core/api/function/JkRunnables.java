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

package dev.jeka.core.api.function;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A mutable container for {@link Runnable}. From this object you can replace the underlying {@link Runnable} or
 * chain it with other ones.
 */
public class JkRunnables implements Runnable {

    private final LinkedList<Entry<Runnable>> entries = new LinkedList<>();

    private boolean log;

    private String taskSuffix = "";

    private JkRunnables() {
    }

    /**
     * Creates a {@link JkRunnables} delegating to a no-op runnable.
     */
    public static JkRunnables of() {
        return new JkRunnables();
    }

    /**
     * Adds the specified {@link Runnable} at the end of the execution chain.
     * @param name The name referencing the runnable. The name can be used to
     *             remove the runnable form the chain or add others with place
     *             relative to this one.
     * @see #remove(String)
     * @see #insertBefore(String, String, Runnable)
     * @see #insertAfter(String, String, Runnable)
     * @see #contains(String)
     */
    public JkRunnables append(String name, Runnable runnable) {
        return append(name, runnable, null);
    }

    /**
     * Adds the specified {@link Runnable}s at the end of the execution chain.
     */
    public JkRunnables append(Runnable ... runnables) {
        for (Runnable runnable : runnables) {
            append(runnable.toString(), runnable);
        }
        return this;
    }

    /**
     * Same as {@link #append(String, Runnable)} but effective only the specified condition
     */
    public JkRunnables appendIf(boolean condition, String name, Runnable runnable) {
        if (condition) {
            return append(name, runnable, null);
        }
        return this;
    }

    /**
     * Inserts the specified runnable in the execution chain, just before another one.
     * @param name The name of the runnable to insert.
     * @param beforeRunnableName The name of the runnable that we must insert before.
     * @param runnable The runnable to insert.
     */
    public JkRunnables insertBefore(String name, String beforeRunnableName, Runnable runnable) {
        return append(name, runnable, new Entry.RelativePlace(beforeRunnableName, Entry.Where.BEFORE));
    }

    /**
     * Replaces a Runnable with the same name in the execution chain if exists,
     * otherwise inserts it before a specified runnable.
     *
     * @param name              The name referencing the runnable.
     * @param beforeRunnableName The name of the runnable that the new runnable should be inserted before.
     * @param runnable          The runnable to replace or insert.
     *
     * @throws IllegalArgumentException if the Runnable is null.
     */
    public JkRunnables replaceOrInsertBefore(String name, String beforeRunnableName, Runnable runnable) {
        if (this.contains(name)) {
            return replaceOrAppend(name, runnable);
        }
        return insertBefore(name, beforeRunnableName, runnable);
    }

    /**
     * Inserts the specified runnable in the execution chain, just after another one.
     * @param name The name of the runnable to insert.
     * @param afterRunnableName The name of the runnable that we must insert after.
     * @param runnable The runnable to insert.
     */
    public JkRunnables insertAfter(String name, String afterRunnableName, Runnable runnable) {
        return append(name, runnable, new Entry.RelativePlace(afterRunnableName, Entry.Where.AFTER));
    }

    /**
     * Removes the runnable having the specified name, from the execution list.
     * @param runnableName The name of the runnable to remove.
     */
    public JkRunnables remove(String runnableName) {
        for (Iterator<Entry<Runnable>> it = entries.iterator(); it.hasNext();) {
            if (it.next().name.equals(runnableName)) {
                it.remove();
            }
        }
        return this;
    }

    /**
     * Replaces the specified {@link Runnable} with the same name in the execution chain, or appends it if not found.
     *
     * @param name     The name referencing the runnable.
     * @param runnable The {@link Runnable} to replace or append.
     *
     * @throws IllegalArgumentException If the {@link Runnable} is null.
     */
    public JkRunnables replaceOrAppend(String name, Runnable runnable) {
        JkUtilsAssert.argument(runnable != null, "Runnable can't be null.");
        boolean found = false;
        for (ListIterator<Entry<Runnable>> it = this.entries.listIterator(); it.hasNext();) {
            Entry entry = it.next();
            if (name.equals(entry.name)) {
                it.remove();
                it.add(entry.withRunnable(runnable));
                found = true;
                break;
            }
        }
        if (!found) {
            append(name, runnable);
        }
        return this;
    }

    /**
     * Returns the name of the {@link Runnable}s, in the order of execution chain.
     */
    public List<String> getRunnableNames() {
        return Entry.sort(entries).stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if the execution chain contains a runnable with the
     * specified name.
     */
    public boolean contains(String runnableName) {
        return entries.stream().anyMatch(entry -> entry.name.equals(runnableName));
    }

    /**
     * Sets if each runnable execution should be wrapped in a log task.
     */
    public JkRunnables setLogTasks(boolean log) {
        this.log = log;
        return this;
    }

    /**
     * Sets the displayed suffix for the task.
     */
    public  JkRunnables setTaskSuffix(String suffix) {
        this.taskSuffix = suffix;
        return this;
    }

    /**
     * Returns the total number of entries in the execution chain.
     */
    public int getSize() {
        return this.entries.size();
    }

    /**
     * Retrieves the {@link Runnable} stored at the specified index in the execution chain.
     */
    public Runnable getRunnable(int index) {
        return this.entries.get(index).runnable;
    }

    /**
     * Executes the {@link Runnable}s, in the order of the execution chain.
     */
    @Override
    public void run() {
        final boolean doLog = log;
        Entry.sort(entries).stream()
                .forEach(entry -> {
                    if (doLog) {
                        JkLog.startTask(entry.name + taskSuffix);
                    }
                    entry.runnable.run();
                    if (doLog) {
                        JkLog.endTask();
                    }
                });
    }

    /**
     * Replaces the current entries, log, and task suffix of this {@code JkRunnables}
     * instance with the entries, log, and task suffix of the specified {@code JkRunnables} instance.
     *
     * @param runnables The {@code JkRunnables} instance whose properties will
     *                  replace the properties in this instance.
     */
    public void fillWith(JkRunnables runnables) {
        this.entries.addAll(runnables.entries);
        this.log = runnables.log;
        this.taskSuffix = runnables.taskSuffix;
    }

    private JkRunnables append(String name, Runnable runnable, Entry.RelativePlace relativePlace) {
        JkUtilsAssert.argument(!this.contains(name), "runnable container contains already an entry named '"
                + name + "'");
        Entry entry = new Entry(name, runnable, relativePlace);
        entries.add(entry);
        Collections.sort(entries);
        return this;
    }

}
