package org.jerkar.api.depmanagement;

import java.io.Serializable;

public interface JkPublishFilter extends Serializable {

	boolean accept(JkVersionedModule versionedModule);

}