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

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for KBean. User code is not supposed to instantiate KBeans using 'new' but usinng
 * {@link JkRunbase#load(java.lang.Class)}.
 */
public abstract class KBean {

    private static final String JKBEAN_CLASS_SIMPLE_NAME = KBean.class.getSimpleName();

    private static final String CLASS_SUFFIX = KBean.class.getSimpleName();

    private final JkRunbase runbase;

    /**
     * Use {@link #init()} instead!
     * <p>
     * Code added here won't work as expected since public fields (such as those from command-line properties,
     * basedir, and imported KBeans) are not yet initialized.
     */
    protected KBean() {
        this(JkRunbase.CURRENT.get());
    }

    private KBean(JkRunbase runbase) {
        this.runbase = runbase;
    }

    /**
     * This method is called by JeKa engine, right after public fields from command-line or properties have been injected.<p>
     * Put your initialization/configuration code here.
     */
    protected void init() {
    }

    /**
     * Returns the base directory of the project. In single projects, base dir = working dir.
     * When working in multi-project (aka multi-module project), the base dir will be
     * the sub-project base directory.
     */
    public final Path getBaseDir() {
        return runbase.getBaseDir();
    }

    /**
     * Resolves the given relative path against the base directory of the project.
     */
    public final Path getBasePath(String relativePath) {
        return getBaseDir().resolve(relativePath);
    }

    /**
     * Returns the name of the folder which stands for the project base directory.
     */
    public final String getBaseDirName() {
        String result = getBaseDir().getFileName().toString();
        return result.isEmpty() ? getBaseDir().toAbsolutePath().getFileName().toString() : result;
    }

    /**
     * Returns the output directory where all the final and intermediate artifacts are generated.
     */
    public final Path getOutputDir() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH);
    }

    /**
     * Returns the {@link JkRunbase} where this KBean has been instantiated.
     */
    public final JkRunbase getRunbase() {
        return runbase;
    }

    /**
     * @see JkRunbase#load(Class)
     */
    public final <T extends KBean> T load(Class<T> beanClass) {
        return runbase.load(beanClass);
    }

    /**
     * @see JkRunbase#find(Class)
     */
    public final <T extends KBean> Optional<T> find(Class<T> beanClass){
        return runbase.find(beanClass);
    }

    static boolean nameMatches(String className, String nameCandidate) {
        if (nameCandidate == null) {
            return false;
        }
        if (nameCandidate.equals(className)) {
            return true;
        }
        String classSimpleName = className.contains(".") ? JkUtilsString.substringAfterLast(className, ".")
                : className;
        String uncapitalizedClassSimpleName = JkUtilsString.uncapitalize(classSimpleName);
        if (JkUtilsString.uncapitalize(nameCandidate).equals(uncapitalizedClassSimpleName)) {
            return true;
        }
        if (className.endsWith(JKBEAN_CLASS_SIMPLE_NAME)) {
            return uncapitalizedClassSimpleName.equals(nameCandidate + KBean.class.getSimpleName());
        }
        return false;
    }

    @Override
    public String toString() {
        return "KBean '" + shortName() + "' [from project '" + JkUtilsPath.friendlyName(this.runbase.getBaseDir()) + "']";
    }

    /**
     * Cleans output directory.
     */
    public KBean cleanOutput() {
        Path output = getOutputDir();
        JkLog.verbose("Clean output directory %s", output.toAbsolutePath().normalize());
        if (Files.exists(output)) {
            JkPathTree.of(output).deleteContent();
        }
        return this;
    }

    final String shortName() {
        return name(this.getClass());
    }

    static String name(String fullyQualifiedClassName) {
        final String className = fullyQualifiedClassName.contains(".")
                ? JkUtilsString.substringAfterLast(fullyQualifiedClassName, ".")
                : fullyQualifiedClassName;
        if (!className.endsWith(CLASS_SUFFIX) || className.equals(CLASS_SUFFIX)) {
            return JkUtilsString.uncapitalize(className);
        }
        final String prefix = JkUtilsString.substringBeforeLast(className, CLASS_SUFFIX);
        return JkUtilsString.uncapitalize(prefix);
    }

    static List<String> acceptedNames(Class<? extends KBean> kbeanClass) {
        List<String> result = new LinkedList<>();
        result.add(kbeanClass.getName());
        result.add(kbeanClass.getSimpleName());
        String uncapitalized = JkUtilsString.uncapitalize(kbeanClass.getSimpleName());
        result.add(uncapitalized);
        if (uncapitalized.endsWith(CLASS_SUFFIX)) {
            result.add(JkUtilsString.substringBeforeFirst(uncapitalized, CLASS_SUFFIX));
        }
        return result;
    }

    static String name(Class<?> kbeanClass) {
        return name(kbeanClass.getName());
    }

    static boolean isPropertyField(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        if (Modifier.isPublic(field.getModifiers())) {
            return true;
        }
        return field.getAnnotation(JkDoc.class) != null;
    }

}

