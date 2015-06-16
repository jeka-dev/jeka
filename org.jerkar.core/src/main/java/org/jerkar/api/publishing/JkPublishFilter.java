package org.jerkar.api.publishing;

import java.io.Serializable;

import org.jerkar.api.depmanagement.JkVersionedModule;

public interface JkPublishFilter extends Serializable {

	boolean accept(JkVersionedModule versionedModule);

}