package org.jerkar.publishing;

import java.io.Serializable;

import org.jerkar.depmanagement.JkVersionedModule;

public interface JkPublishFilter extends Serializable {

	boolean accept(JkVersionedModule versionedModule);

}