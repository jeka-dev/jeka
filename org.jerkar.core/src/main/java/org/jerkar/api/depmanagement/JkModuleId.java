package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Comparator;

import org.jerkar.api.utils.JkUtilsString;

/**
 * Identifier for project. The identifier will be used to name the generated
 * artifacts and as a moduleId for Maven or Ivy.
 * 
 * @author Jerome Angibaud
 */
public final class JkModuleId implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static Comparator<JkModuleId> GROUP_NAME_COMPARATOR = new GroupAndNameComparator();

    /**
     * Creates a project id according the specified group and name.
     */
    public static JkModuleId of(String group, String name) {
        return new JkModuleId(group, name);
    }

    /**
     * Creates a project id according a string supposed to be formatted as
     * <code>group</code>.<code>name</code> or <code>group</code>:
     * <code>name</code>. The last '.' is considered as the separator between
     * the group and the name. <br/>
     * If there is no '.' then the whole string will serve both for group and
     * name.
     */
    public static JkModuleId of(String groupAndName) {
        if (groupAndName.contains(":")) {
            final String group = JkUtilsString.substringBeforeLast(groupAndName, ":");
            final String name = JkUtilsString.substringAfterLast(groupAndName, ":");
            return new JkModuleId(group, name);
        }
        if (groupAndName.contains(".")) {
            final String group = JkUtilsString.substringBeforeLast(groupAndName, ".");
            final String name = JkUtilsString.substringAfterLast(groupAndName, ".");
            return new JkModuleId(group, name);
        }
        return new JkModuleId(groupAndName, groupAndName);
    }

    private final String group;

    private final String name;

    private JkModuleId(String group, String name) {
        super();
        this.group = group;
        this.name = name;
    }

    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        if (group.equals(name)) {
            return name;
        }
        return group + "." + name;
    }

    /**
     * Returns a string formatted as 'group:name'.
     */
    public String groupAndName() {
        return group + ":" + name;
    }

    /**
     * Creates a {@link JkVersionedModule} from this module and the specified
     * version.
     */
    public JkVersionedModule version(String version) {
        return JkVersionedModule.of(this, JkVersion.ofName(version));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JkModuleId other = (JkModuleId) obj;
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!group.equals(other.group)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return fullName();
    }

    private static class GroupAndNameComparator implements Comparator<JkModuleId> {

        @Override
        public int compare(JkModuleId o1, JkModuleId o2) {
            if (o1.group.equals(o2.group)) {
                return o1.name.compareTo(o2.name);
            }
            return o1.group.compareTo(o2.group);
        }

    }

}
