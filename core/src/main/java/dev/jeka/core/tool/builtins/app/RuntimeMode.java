/*
 * Copyright 2014-2026  the original author or authors.
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

package dev.jeka.core.tool.builtins.app;

/**
 * Defines the runtime mode for app installation.
 * <ul>
 *   <li><b>JVM</b>: JVM application running on a JDK managed by Jeka. Binary located in ~/.jeka/apps</li>
 *   <li><b>NATIVE</b>: Native executable compiled ahead-of-time. Binary located in ~/.jeka/apps</li>
 *   <li><b>BUNDLE</b>: Application bundled with a tailored JRE, installed in user-specified location</li>
 * </ul>
 */
enum RuntimeMode {
    /**
     * JVM application running on Jeka-managed JDK (installed in ~/.jeka/apps)
     */
    JVM,

    /**
     * Native executable compiled ahead-of-time (installed in ~/.jeka/apps)
     */
    NATIVE,

    /**
     * Application bundled with tailored JRE (installed in user-specified location)
     */
    BUNDLE
}
