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

import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.system.JkInfo;

/**
 * Holds constants about project structures
 */
public final class JkConstants {

    // ------------ Standard file names and locations --------------------------

    /**
     * Relative path to the project base directory where output files are generated.
     */
    public static final String OUTPUT_PATH = "jeka-output";

    /**
     * Relative path to the project base directory where jeka work files are generated.
     */
    public static final String JEKA_WORK_PATH = ".jeka-work";

    /**
     * Relative path to put jars that will be automatically prepended to jeka classpath.
     */
    static final String JEKA_BOOT_DIR = "jeka-boot";

    /**
     * Relative path to the project where the jeka-src classes will be compiled.
     */
    public static final String JEKA_SRC_CLASSES_DIR = JEKA_WORK_PATH + "/jeka-src-classes";

    /**
     * Relative path of jeka-src dir to the base dir.
     */
    public static final String JEKA_SRC_DIR = "jeka-src";

    /**
     * Relative path to the jeka.properties.
     */
    public static final String PROPERTIES_FILE = "jeka.properties";

    // ------------ Jeka standard properties --------------------------

    static final String CMD_PREFIX_PROP = "jeka.cmd.";

    static final String CMD_APPEND_SUFFIX_PROP =  "_append";

    public static final String CLASSPATH_INJECT_PROP = "jeka.inject.classpath";


    /**
     * @deprecated Use {@link #KBEAN_DEFAULT_PROP} instead.
     */
    @Deprecated
    public static final String DEFAULT_KBEAN_PROP = "jeka.default.kbean";

    public static final String KBEAN_DEFAULT_PROP = "jeka.kbean.default";

    public static final String KBEAN_LOCAL_PROP = "jeka.kbean.local";

    static final String CMD_APPEND_PROP = CMD_PREFIX_PROP + CMD_APPEND_SUFFIX_PROP;

    static final String CMD_SUBSTITUTE_SYMBOL = "::";

    // --------------------  Misc ----------------------------------------------

    public static final JkPathMatcher PRIVATE_IN_DEF_MATCHER = JkPathMatcher.of("_*", "_*/**");

    /*
     * If version is not specified for dependencies of 'dev.jeka' group, then use the running Jeka version
     */
    public static final JkVersionProvider JEKA_VERSION_PROVIDER = JkVersionProvider.of("dev.jeka:*",
            JkInfo.getJekaVersion());

    static final String KBEAN_CMD_SUFFIX = ":";

    static final String KBEAN_CLASS_NAMES_CACHE_FILE = "jeka-kbean-classes.txt";

    static final String KBEAN_CLASSPATH_CACHE_FILE =  "jeka-kbean-classpath.txt";
}
