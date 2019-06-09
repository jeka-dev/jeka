package dev.jeka.core.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import dev.jeka.core.api.utils.JkUtilsIterable;

/**
 * Information about excluding artifacts or whole modules.
 *
 * @author Jerome Angibaud
 */
public final class JkDepExclude implements Serializable {

    private static final long serialVersionUID = 1L;

    private final JkModuleId moduleId;

    private final String type;

    private final String ext;

    private final Set<JkScope> scopes;

    private JkDepExclude(JkModuleId moduleId, String type, String ext, Set<JkScope> scopes) {
        super();
        this.moduleId = moduleId;
        this.type = type;
        this.ext = ext;
        this.scopes = scopes;
    }

    /**
     * Creates an exclusion of the specified module.
     */
    @SuppressWarnings("unchecked")
    public static JkDepExclude of(JkModuleId moduleId) {
        return new JkDepExclude(moduleId, null, null, Collections.EMPTY_SET);
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDepExclude of(String group, String name) {
        return of(JkModuleId.of(group, name));
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDepExclude of(String groupAndName) {
        return of(JkModuleId.of(groupAndName));
    }

    /**
     * Returns an exclusion identical to this one but with the specified type.
     * Types generally corresponds to getExtension or classifier but not always.
     * Some examples are <i>jar</i>, <i>test-jar</i>, <i>test-client</i>.
     */
    public JkDepExclude withType(String typeArg) {
        return new JkDepExclude(moduleId, typeArg, ext, scopes);
    }

    /**
     * Returns an exclusion identical to this one but with the specified getExtension.
     * Types generally corresponds to getExtension or classifier but not always.
     * Some examples are <i>jar</i>, <i>test-jar</i>, <i>test-client</i>.
     */
    public JkDepExclude withExt(String extension) {
        return new JkDepExclude(moduleId, type, extension, scopes);
    }

    /**
     * Returns a exclusion identical to this one but narrowed to the specified scopes.
     * When some scopes are defined, the exclusion is effective only if the dependency
     * likely to hold the module to exclude is declared with one of the specified scopes.
     */
    public JkDepExclude withScopes(JkScope... scopes) {
        return new JkDepExclude(moduleId, type, ext, Collections.unmodifiableSet(JkUtilsIterable.setOf(scopes)));
    }

    /**
     * Returns the module id to exclude.
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Returns the type of the artifact file to exclude.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the getExtension for the artifact files to exclude. If not <code>null</code>
     * only file artifact having this getExtension will be effectively excluded.
     */
    public String getExt() {
        return ext;
    }

    /**
     * Returns the scopes that render the exclusion effective.
     * @see #withScopes(JkScope...)
     */
    public Set<JkScope> getScopes() {
        return scopes;
    }

}
