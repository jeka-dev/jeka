package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;

import java.nio.file.Path;

/** Loads a KBean on a directory, without executing any command. */
public class KBeanTestLoader {

    public static <T extends KBean> T load(Path baseDir, Class<T> kbeanClass) {
        BehaviorSettings initialSettings = BehaviorSettings.INSTANCE;
        ClassLoader initialClassloader = Thread.currentThread().getContextClassLoader();
        BehaviorSettings.INSTANCE = BehaviorSettings.ofDefault();
        try {
            Engine engine = Engine.of(true, baseDir, JkRepo.ofMavenCentral().toSet(), JkDependencySet.of());
            engine.resolveKBeans();
            Engines.registerMaster(engine);
            Thread.currentThread().setContextClassLoader(
                    new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
            return engine.getOrCreateRunbase(new KBeanAction.Container(), true).load(kbeanClass);
        } finally {
            BehaviorSettings.INSTANCE = initialSettings;
            Thread.currentThread().setContextClassLoader(initialClassloader);
        }
    }

}
