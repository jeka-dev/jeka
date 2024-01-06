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

    private final LinkedList<Entry> entries = new LinkedList<>();

    private boolean log;

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
     * Sets the specified {@link Runnable}s as the execution chain. <p>
     * This removes all pre-existing runnables from the chain.
     */
    public JkRunnables set(Runnable ... runnables) {
        this.entries.clear();
        return this.append(runnables);
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
        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            if (it.next().name.equals(runnableName)) {
                it.remove();
            }
        }
        return this;
    }

    /**
     * Returns the name of the {@link Runnable}s, in the order of execution chain.
     */
    public List<String> getRunnableNames() {
        return entries.stream().map(entry -> entry.name).collect(Collectors.toList());
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
     * Executes the {@link Runnable}s, in the order of the execution chain.
     */
    @Override
    public void run() {
        final boolean doLog = log;
        entries.forEach(entry -> {
            if (doLog) {
                JkLog.startTask("running " + entry.name);
            }
            entry.runnable.run();
            if (doLog) {
                JkLog.endTask();
            }
        });
    }

    private JkRunnables append(String name, Runnable runnable, Entry.RelativePlace relativePlace) {
        JkUtilsAssert.argument(!this.contains(name), "runnable container contains already an entry named '"
                + name + "'");
        Entry entry = new Entry(name, runnable, relativePlace);
        entries.add(entry);
        Collections.sort(entries);
        return this;
    }

    private static class Entry implements Comparable<Entry> {

        final String name;

        final Runnable runnable;

        // Nullable
        final RelativePlace relativePlace;

        @Override
        public int compareTo(Entry other) {
            if (!hasRelationWith(other)) {
                return 0;
            }
            if (hasRelationConflictWith(other)) {
                throw new IllegalStateException("Entries " + this + " and " + other
                        + " have relative position in conflict with each other.");
            }
            if (isRelativeTo(other.name)) {
                return relativePlace.where == Where.BEFORE ? -1 : 1;
            }
            return other.relativePlace.where == Where.BEFORE ? 1 : -1;
        }

        enum Where {
            BEFORE, AFTER
        }

        Entry(String name, Runnable runnable, RelativePlace relativePlace) {
            JkUtilsAssert.argument(name != null, "runnable name cannot be null");
            JkUtilsAssert.argument(runnable != null, "runnable cannot be null");
            this.name = name;
            this.runnable = runnable;
            this.relativePlace = relativePlace;
        }

        Entry(Runnable runnable) {
            this(runnable.toString(), runnable, null);
        }

        boolean isRelativeTo(String entryName) {
            return relativePlace!= null && relativePlace.relativeEntryName.equals(entryName);
        }

        boolean hasRelationWith(Entry other) {
            return isRelativeTo(other.name) || other.isRelativeTo(name);
        }

        boolean hasRelationConflictWith(Entry other) {
            return isRelativeTo(other.name) && other.isRelativeTo(name)
                    && relativePlace.where.equals(other.relativePlace.where);
        }

        @Override
        public String toString() {
            return name + (relativePlace == null ? "" : " (" + relativePlace + ")");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return name.equals(entry.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        static class RelativePlace {

            public RelativePlace(String name, Where where) {
                JkUtilsAssert.argument(name != null, "relative runnable name cannot be null");
                JkUtilsAssert.argument(where != null, "where position cannot be null");
                this.relativeEntryName = name;
                this.where = where;
            }

            final String relativeEntryName;

            final Where where;

            @Override
            public String toString() {
                return where + " " + relativeEntryName;
            }
        }

    }

}
