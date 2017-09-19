package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Marker interface for a dependency. It can be a either :
 * <ul>
 * <li>An external module as <code>org.hibernate:hibernate-core:3.0.+</code></li>
 * <li>A project inside a multi-project build</li>
 * <li>Some files on the file system</li>
 * </ul>
 * Each dependency is associated with a scope mapping to determine precisely in
 * which scenario the dependency is necessary.
 *
 * @author Jerome Angibaud
 */
public interface JkDependency extends Serializable {


}
