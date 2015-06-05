package org.jerkar.publishing;

import org.jerkar.depmanagement.JkVersionedModule;

public interface JkPublishFilter {

	boolean accept(JkVersionedModule versionedModule);

}