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

package dev.jeka.core.api.kotlin;

import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

public class JkKotlinModules {

    private JkKotlinModules() {
    }
    public static final String GROUP = "org.jetbrains.kotlin";

    public static final String STDLIB = "org.jetbrains.kotlin:kotlin-stdlib";

    public static final String STDLIB_JS= "org.jetbrains.kotlin:kotlin-stdlib-js";

    public static final String STDLIB_JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";

    public static final String STDLIB_JDK7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7";

    public static final String STDLIB_COMMON = "org.jetbrains.kotlin:kotlin-stdlib-common";

    public static final String ANDROID_EXTENSION_RUNTIME = "org.jetbrains.kotlin:kotlin-android-extension-runtime";

    public static final String REFLECT = "org.jetbrains.kotlin:kotlin-reflect";

    public static final String TEST = "org.jetbrains.kotlin:kotlin-test";

    public static final String TEST_JUNIT = "org.jetbrains.kotlin:kotlin-test-junit";

    public static final String TEST_JUNIT5= "org.jetbrains.kotlin:kotlin-test-junit5";

    public static final String TEST_COMMON = "org.jetbrains.kotlin:kotlin-test-common";

    public static final String TEST_ANNOTATIONS_COMMON =  "org.jetbrains.kotlin:kotlin-test-annotations-common";

    public static final String COMPILER = "org.jetbrains.kotlin:kotlin-compiler";

    public static final String COMPILER_EMBEDDABLE = "org.jetbrains.kotlin:kotlin-compiler-embeddable";

    public static final String COMPILER_PLUGIN_ALLOPEN = "org.jetbrains.kotlin:kotlin-allopen";

    public static final String COMPILER_PLUGIN_ALLOPEN_ID = "all-open";

    public static final String COMPILER_PLUGIN_NOARG = "org.jetbrains.kotlin:kotlin-noarg";

    public static final String COMPILER_PLUGIN_NOARG_ID = "org.jetbrains.kotlin:no-arg";

    public static final String COMPILER_PLUGIN_SAM_WITH_RECEIVER = "org.jetbrains.kotlin:kotlin-sam-with-receiver";

    public static final String COMPILER_PLUGIN_SAM_WITH_RECEIVER_ID = "sam-with-receiver";

    public static final String COMPILER_PLUGIN_KAPT = "org.jetbrains.kotlin:kotlin-annotation-processing";

    public static final String COMPILER_PLUGIN_KAPT_ID = "org.jetbrains.kotlin.kapt3";

    public static final String COMPILER_PLUGIN_KOTLINX_SERIALIZATION = "org.jetbrains.kotlin:kotlin-serialization-unshaded";

    public static JkVersionProvider versionProvider(String kotlinVersion) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(kotlinVersion), "kotlin version cannot be blank.");
        return JkVersionProvider.of()
                .and(STDLIB, kotlinVersion)
                .and(STDLIB_JS, kotlinVersion)
                .and(STDLIB_JDK8, kotlinVersion)
                .and(STDLIB_JDK7, kotlinVersion)
                .and(STDLIB_COMMON, kotlinVersion)
                .and(REFLECT, kotlinVersion)
                .and(ANDROID_EXTENSION_RUNTIME, kotlinVersion)
                .and(TEST, kotlinVersion)
                .and(TEST_COMMON, kotlinVersion)
                .and(TEST_ANNOTATIONS_COMMON, kotlinVersion)
                .and(TEST_JUNIT, kotlinVersion)
                .and(TEST_JUNIT5, kotlinVersion)
                .and(COMPILER, kotlinVersion)
                .and(COMPILER_EMBEDDABLE, kotlinVersion);
    }




}
