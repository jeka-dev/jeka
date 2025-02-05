/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A KBean can import one or several KBeans from external projects.
 * This class holds imported KBean by a given KBean instance.
 *
 * @author Jerome Angibaud
 */
public final class JkImportedKBeans {

    private final KBean holder;

    private List<KBean> directs;

    private List<KBean> transitives;

    JkImportedKBeans(KBean holder) {
        this.holder = holder;
        this.directs = computeDirects(holder);
    }

    /**
     * Returns imported KBeans.
     */
    public List<KBean> get(boolean includeTransitives) {
        return includeTransitives
                ? Optional.ofNullable(transitives).orElseGet(() -> (transitives = computeTransitives(new HashSet<>())))
                : Optional.ofNullable(directs).orElseGet(() -> (directs = computeDirects(holder)));
    }

    /**
     * Returns KBeans found in imported runbases having the specified type.
     */
    public <T extends KBean> List<T> get(Class<T> beanClass, boolean includeTransitives) {
        return get(includeTransitives).stream()
                .map(KBean::getRunbase)
                .map(runbase -> runbase.find(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Loads and returns the KBeans of the specified type from the imported runbases.
     */
    public <T extends KBean> List<T> load(Class<T> beanClass, boolean includeTransitives) {
        return get(includeTransitives).stream()
                .map(KBean::getRunbase)
                .map(runbase -> runbase.load(beanClass))
                .collect(Collectors.toList());
    }

    private List<KBean> computeTransitives(Set<Path> files) {
        final List<KBean> result = new LinkedList<>();
        for (final KBean kBean : directs) {
            final Path dir = kBean.getBaseDir();
            if (!files.contains(dir)) {
                result.addAll(kBean.getImportedKBeans().computeTransitives(files));
                result.add(kBean);
                files.add(dir);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<KBean> computeDirects(KBean masterBean) {
        final List<KBean> result = new LinkedList<>();
        final List<Field> fields = JkUtilsReflect.getDeclaredFieldsWithAnnotation(masterBean.getClass(), JkInject.class);
        fields.addAll(JkUtilsReflect.getDeclaredFieldsWithAnnotation(masterBean.getClass(), JkInjectRunbase.class));
        if (!fields.isEmpty()) {
            JkLog.verbose("Projects imported by %s : %s", masterBean, fields);
        }
        for (final Field field : fields) {
            String importedDir = Injects.getImportedDir(field);
            if (importedDir == null) {
                continue;  // THis means that's a local KBean that should be handled at Runbase init level
            }
            final KBean importedJkClass = createImportedKBean(
                    (Class<? extends KBean>) field.getType(), importedDir, masterBean.getBaseDir());
            try {
                JkUtilsReflect.setFieldValue(masterBean, field, importedJkClass);
            } catch (final RuntimeException e) {
                Path currentClassBaseDir = Paths.get(masterBean.getClass().getProtectionDomain()
                        .getCodeSource().getLocation().getPath());
                while (!Files.exists(currentClassBaseDir.resolve(JkConstants.JEKA_SRC_DIR)) && currentClassBaseDir != null) {
                    currentClassBaseDir = currentClassBaseDir.getParent();
                }
                if (!Files.exists(currentClassBaseDir)) {
                    throw new IllegalStateException("Can't inject imported run instance of type "
                            + importedJkClass.getClass().getSimpleName()
                            + " into field " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " from directory " + masterBean.getBaseDir()
                            + " while working dir is " + Paths.get("").toAbsolutePath());
                }
                throw new IllegalStateException("Can't inject imported run instance of type "
                        + importedJkClass.getClass().getSimpleName()
                        + " into field " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " from directory " + masterBean.getBaseDir()
                        + "\nJeka class is located in " + currentClassBaseDir
                        + " while working dir is " + Paths.get("").toAbsolutePath()
                        + ".\nPlease set working dir to " + currentClassBaseDir, e);
            }
            if (!JkUtilsString.isBlank(importedDir) && !".".equals(importedDir)) {
                result.add(importedJkClass);
            }
        }
        return result;
    }

    /*
     * Creates an instance of <code>JkBean</code> for the given project and
     * Jeka class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private static <T extends KBean> T createImportedKBean(Class<T> importedBeanClass,
                                                           String relativePath,
                                                           Path holderBaseDir) {
        final Path importedProjectDir = holderBaseDir.resolve(relativePath).normalize();
        JkLog.verboseStartTask("Import bean " + importedBeanClass.getName() + " from " + importedProjectDir);

        // Not sure if it is necessary. Is so, explain why.
        Path originalContextDir = JkRunbase.getCurrentContextBaseDir().getBaseDir();
        JkRunbase.setBaseDirContext(importedProjectDir);
        JkRunbase runbase = JkRunbase.get(importedProjectDir);

        // initialize the runbase with empty cmd
        JkLog.startTask("Initialize runbase " + JkRunbase.getMaster().getBaseDir().relativize(runbase.getBaseDir()));
        runbase.init(new KBeanAction.Container());
        JkLog.endTask();
        final T result = runbase.load(importedBeanClass);
        JkRunbase.setBaseDirContext(Optional.ofNullable(originalContextDir).orElse(Paths.get("")));

        JkLog.verboseEndTask();
        return result;
    }

}
