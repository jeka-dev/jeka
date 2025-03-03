/*
 * Copyright 2014-2025  the original author or authors.
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

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

// non-private for testing purpose
class DefaultAndImplicitKBean {

    final String defaultKBeanClassName;

    final String implicitKbeanClassName;
    
    DefaultAndImplicitKBean(String defaultKBeanClassName, String implicitKBeanClassName) {
        this.defaultKBeanClassName = defaultKBeanClassName;
        this.implicitKbeanClassName = implicitKBeanClassName;

    }

    DefaultAndImplicitKBean(List<String> kbeanClassNames,
                            List<String> localKbeanClassNames,
                            String defaultKBeanClassName,
                            String implicitKBeanName) {

        // First get the local KBean
        if (implicitKBeanName != null) {
            implicitKbeanClassName = firstMatchingClassname(localKbeanClassNames, implicitKBeanName).orElseThrow(
                    () -> {
                        String message = String.format("No Kbean class found in jeka-src matching %s name.",
                                implicitKBeanName);
                        if (LogSettings.INSTANCE.debug) {
                            message = message + "\nLocal available KBeans are: " + localKbeanClassNames;
                        }
                        return new JkException(message);
                    }
            );
        } else {
            implicitKbeanClassName = localKbeanClassNames.stream().findFirst().orElse(null);
        }

        // Get the default KBean
        String defaultKBeanName = BehaviorSettings.INSTANCE.defaultKbeanName.orElse(defaultKBeanClassName);
        if (defaultKBeanName == null) {
            this.defaultKBeanClassName = implicitKbeanClassName;
        } else {
            this.defaultKBeanClassName = firstMatchingClassname(kbeanClassNames, defaultKBeanName)
                    .orElse(localKbeanClassNames.stream().findFirst().orElse(null));
            JkLog.debug("Default KBean Class Name : " + this.defaultKBeanClassName);
            if (this.defaultKBeanClassName == null) {
                JkLog.warn("Specified default KBean '%s' not found among KBeans %s", defaultKBeanName, kbeanClassNames);
            }
        }
    }

    static DefaultAndImplicitKBean of(boolean isMasterEngine, JkProperties properties, List<String> kbeanClassNames, List<String> localKbeanClassNames) {
        String defaultKBeanName = Optional.ofNullable(properties.get(JkConstants.KBEAN_DEFAULT_PROP))
                .orElse(properties.get(JkConstants.DEFAULT_KBEAN_PROP));  // for backward compatibility

        String implicitKBeanName = isMasterEngine ? Optional.ofNullable(properties.get(JkConstants.KBEAN_LOCAL_PROP))
                .orElse(null) : null;

        return new DefaultAndImplicitKBean(kbeanClassNames, localKbeanClassNames, defaultKBeanName, implicitKBeanName);
    }

    private static Optional<String> firstMatchingClassname(List<String> classNames, String candidate) {
        return classNames.stream()
                .filter(className -> KBean.nameMatches(className, candidate))
                .findFirst();
    }
}
