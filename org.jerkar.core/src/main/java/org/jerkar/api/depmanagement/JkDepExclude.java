package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Information about excluding artifacts or whole modules.
 * 
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

    @SuppressWarnings("unchecked")
    public static JkDepExclude of(JkModuleId moduleId) {
        return new JkDepExclude(moduleId, null, null, Collections.EMPTY_SET);
    }

    public static JkDepExclude of(String group, String name) {
        return of(JkModuleId.of(group, name));
    }

    public static JkDepExclude of(String groupAndName) {
        return of(JkModuleId.of(groupAndName));
    }

    public JkDepExclude type(String typeArg) {
        return new JkDepExclude(moduleId, typeArg, ext, scopes);
    }

    public JkDepExclude ext(String extArg) {
        return new JkDepExclude(moduleId, type, ext, scopes);
    }

    public JkDepExclude scopes(JkScope... scopes) {
        return new JkDepExclude(moduleId, type, ext, JkUtilsIterable.setOf(scopes));
    }

    public JkModuleId moduleId() {
        return moduleId;
    }

    public String type() {
        return type;
    }

    public String ext() {
        return ext;
    }

    public Set<JkScope> getScopes() {
        return scopes;
    }

}
