/*
 * Copyright 2014-2025  the original author or authors.
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

import dev.jeka.core.api.utils.JkUtilsAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class Entry<T> implements Comparable<Entry<T>> {

    final String name;

    final T runnable;

    // Nullable
    final RelativePlace relativePlace;

    enum Where {
        BEFORE, AFTER
    }

    Entry(String name, T runnable, RelativePlace relativePlace) {
        JkUtilsAssert.argument(name != null, "runnable name cannot be null");
        JkUtilsAssert.argument(runnable != null, "runnable cannot be null");
        this.name = name;
        this.runnable = runnable;
        this.relativePlace = relativePlace;
    }

    Entry<T> withRunnable(T runnable) {
        return new Entry<T>(this.name, runnable, this.relativePlace);
    }

    @Override
    public int compareTo(Entry<T> other) {
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

    boolean isRelativeTo(String entryName) {
        return relativePlace != null && relativePlace.relativeEntryName.equals(entryName);
    }

    boolean hasRelationWith(Entry<?> other) {
        return isRelativeTo(other.name) || other.isRelativeTo(name);
    }

    boolean hasRelationConflictWith(Entry<T> other) {
        return isRelativeTo(other.name) && other.isRelativeTo(name)
                && relativePlace.where.equals(other.relativePlace.where);
    }

    Entry<?> findRelationShip(List<Entry<?>> entries) {
        return entries.stream()
                .filter(e -> e.hasRelationWith(this))
                .findFirst().orElse(null);
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

    static <T> List<Entry<T>> sort(List<Entry<T>> entries) {
        List sorted = new ArrayList<>();

        // First add non relative entry
        entries.stream()
                .filter(entry -> entry.relativePlace == null)
                .forEach(sorted::add);

        for (Entry entry : entries) {
            if (entry.relativePlace == null) {
                continue;
            }
            Entry<?> relativeExistingEntry = entry.findRelationShip(sorted);
            if (relativeExistingEntry == null) {
                sorted.add(entry);
                continue;
            }
            int compare = entry.compareTo(relativeExistingEntry);
            if (compare < 0) {
                int index = sorted.indexOf(relativeExistingEntry);
                sorted.add(index, entry);
            } else {
                sorted.add(entry);
            }
        }
        return sorted;
    }

}
