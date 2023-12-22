package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Comparator;

/**
 * Identify a module (such as <a href="https://mvnrepository.com/artifact/com.google.guava/guava">Google Guava</a>).
 * in a binary repository (such as a Maren repository).
 *
 * The module is identified by a group and a name which are both mandatory.
 *
 * Each module have generally many versions, and each version may contain many artifacts (which are identified
 * using {@link JkCoordinate).}
 */
public final class JkModuleId implements Comparator<JkModuleId> {

    private final String group;

    private final String name;


    /**
     * Creates a module id according the specified group and name.
     */
    public static JkModuleId of(String group, String name) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(group), "Group can't be empty");
        JkUtilsAssert.argument(!JkUtilsString.isBlank(name), "Name can't be empty");
        return new JkModuleId(group, name);
    }

    /**
     * Creates a module id according a string supposed to be formatted as
     * <code>group</code>.<code>name</code> or <code>group</code>:
     * <code>name</code>. The last '.' is considered as the separator between
     * the group and the name. <br/>
     * If there is no '.' then the whole string will serve both for group and
     * name.
     */
    public static JkModuleId of(@JkDepSuggest String moduleId) {
        if (moduleId.contains(":")) {
            String[] items = moduleId.split(":");
            final String group = items[0].trim();
            final String name = items[1].trim();
            return JkModuleId.of(group, name);
        }
        if (moduleId.contains(".")) {
            final String group = JkUtilsString.substringBeforeLast(moduleId, ".").trim();
            final String name = JkUtilsString.substringAfterLast(moduleId, ".").trim();
            return JkModuleId.of(group, name);
        }
        return JkModuleId.of(moduleId, moduleId);
    }

    private JkModuleId(String group, String name) {
        super();
        this.group = group;
        this.name = name;
    }

    /**
     * Group of this module.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Name of this module.
     */
    public String getName() {
        return name;
    }

    /**
     * A concatenation of the group and name of the module as '[group].[name]'.
     */
    public String getDotNotation() {
        if (group.equals(name)) {
            return name;
        }
        return group + "." + name;
    }

    /**
     * A concatenation of the group and name of this module as '[group]:[value]'.
     */
    public String getColonNotation() {
        return group + ":" + name;
    }

    /**
     * Creates a {@link JkCoordinate} from this moduleId with the specified version.
     */
    public JkCoordinate toCoordinate(@JkDepSuggest String version) {
        return toCoordinate(JkVersion.of(version));
    }

    /**
     * Creates a {@link JkCoordinate} from this moduleId with the specified version.
     */
    public JkCoordinate toCoordinate(JkVersion version) {
        return JkCoordinate.of(this, version);
    }

    /**
     * Creates a {@link JkCoordinate} from this moduleId with unspecified version.
     */
    public JkCoordinate toCoordinate() {
        return toCoordinate(JkVersion.UNSPECIFIED);
    }

    @Override
    public String toString() {
        return getColonNotation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkModuleId that = (JkModuleId) o;

        if (!group.equals(that.group)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = group.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public int compare(JkModuleId o1, JkModuleId o2) {
        if (o1.group.equals(o2.group)) {
            return o1.name.compareTo(o2.name);
        }
        return o1.group.compareTo(o2.group);
    }
}
