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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;

import java.util.List;
import java.util.Optional;

// non-private for testing purpose
class DefaultAndInitKBean {

    final String initKbeanClassName;

    final String defaultKBeanClassName;

    DefaultAndInitKBean(List<String> kbeanClassNames, List<String> localKbeanClassNames, String defaultBeanName) {
        String defaultKBeanName = BehaviorSettings.INSTANCE.kbeanName.orElse(defaultBeanName);

        JkLog.debug("Default KBean Name : " + defaultKBeanName);
        defaultKBeanClassName = firstMatchingClassname(kbeanClassNames, defaultKBeanName)
                .orElse(localKbeanClassNames.stream().findFirst().orElse(null));
        JkLog.debug("Default KBean Class Name : " + defaultKBeanClassName);
        if (defaultKBeanName != null && defaultKBeanClassName == null) {
            JkLog.warn("Specified default KBean '%s' not found among KBeans %s", defaultKBeanName, kbeanClassNames);
        }

        // The first KBean to be initialized
        initKbeanClassName = localKbeanClassNames.stream().findFirst().orElse(null);

    }

    static DefaultAndInitKBean of(JkProperties properties, List<String> kbeanClassNames, List<String> localKbeanClassNames) {
        String defaultKBeanName = Optional.ofNullable(properties.get(JkConstants.KBEAN_DEFAULT_PROP))
                                .orElse(properties.get(JkConstants.DEFAULT_KBEAN_PROP));

        return new DefaultAndInitKBean(kbeanClassNames, localKbeanClassNames, defaultKBeanName);

    }

    private static Optional<String> firstMatchingClassname(List<String> classNames, String candidate) {
        return classNames.stream()
                .filter(className -> KBean.nameMatches(className, candidate))
                .findFirst();
    }
}
