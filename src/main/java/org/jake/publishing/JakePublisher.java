package org.jake.publishing;

import java.util.Date;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.depmanagement.ivy.JakeIvy;

public final class JakePublisher {

    private final JakeIvy jakeIvy;

    private JakePublisher(JakeIvy jakeIvy) {
        super();
        this.jakeIvy = jakeIvy;
    }

    public static JakePublisher usingIvy(JakeIvy ivy) {
        return new JakePublisher(ivy);
    }

    public void publishIvy(JakeVersionedModule versionedModule, JakeIvyPublication publication, JakeDependencies dependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping, Date deliveryDate) {
        this.jakeIvy.publish(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
    }

    public void publishMaven(JakeVersionedModule versionedModule, JakeMavenPublication publication, JakeDependencies dependencies, Date deliveryDate) {
        this.jakeIvy.publish(versionedModule, publication, dependencies, deliveryDate);
    }

    public boolean hasMavenPublishRepo() {
        return this.jakeIvy.hasMavenPublishRepo();
    }

    public boolean hasIvyPublishRepo() {
        return this.jakeIvy.hasIvyPublishRepo();
    }


}
