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

// non-private for testing purpose
class DefaultKBeanResolver {


    static String get(boolean isMaster, JkProperties properties,
                      List<String> kbeanClassNames,
                      List<String> localKbeanClassNames) {

        String defaultKBeanName = null;
        if (isMaster) {  // The --kbean option is only taken in account for the master base.
            defaultKBeanName = BehaviorSettings.INSTANCE.defaultKbeanName.orElse(null);
        }
        if (defaultKBeanName == null) {
            defaultKBeanName = properties.get(JkConstants.KBEAN_DEFAULT_PROP);
        }
        if (defaultKBeanName == null) {
            defaultKBeanName =  properties.get(JkConstants.DEFAULT_KBEAN_PROP);
        }
        if (defaultKBeanName == null) {
            return localKbeanClassNames.stream().findFirst().orElse(null);
        }
        String finalDefaultKBeanName = defaultKBeanName;
        String result =  kbeanClassNames.stream()
                .filter(className -> KBean.nameMatches(className, finalDefaultKBeanName))
                .findFirst().orElse(null);
        if (result == null) {
            throw new JkException("Specified default KBean '%s' not found among KBeans %s", defaultKBeanName,
                    kbeanClassNames);
        } else {
            JkLog.debug("Default KBean Class Name : " + result);
        }
        return result;
    }


}
