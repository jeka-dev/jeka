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

import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Injects {

    static List<Class<? extends KBean>> getLocalInjectedClasses(Class<? extends KBean> kbeanClass) {
        return getInjectFields(kbeanClass, true).stream()
                .map(Field::getType)
                .map(clazz -> (Class<? extends KBean>) clazz)
                .collect(Collectors.toList());
    }

    static String getImportedDir(Field field) {
        String importedDir;
        final JkInject jkInject = field.getAnnotation(JkInject.class);
        if (jkInject != null) {
            importedDir = jkInject.value();
        } else {
            final JkInjectRunbase jkInjectRunbase = field.getAnnotation(JkInjectRunbase.class);
            if (jkInjectRunbase != null) {
                importedDir = jkInjectRunbase.value();
            } else {
                importedDir = null;
            }
        }
        if (JkUtilsString.isBlank(importedDir)) {
            importedDir = null;
        }
        return importedDir;
    }

    static void injectLocalKBeans(KBean kbeanUnderInitialization) {
        Field[] fields = kbeanUnderInitialization.getClass().getDeclaredFields();
        for (Field field : fields) {
            JkInject jkInject = field.getAnnotation(JkInject.class);
            if (jkInject != null && JkUtilsString.isBlank(jkInject.value())) {
                Class<?> type = field.getType();
                if (!KBean.class.isAssignableFrom(type)) {
                    String msg = String.format("A Field annotated with @JkInject should be of type or subtype " +
                            "of KBean. Was: %s.%nPlease fix this in the code: %s.", type, field);
                    throw new RuntimeException(msg);
                }
                JkRunbase runbase = kbeanUnderInitialization.getRunbase();
                KBean injectedValue;
                if (runbase.isInitialized()) {
                    injectedValue = runbase.load((Class<? extends KBean>) type);
                } else {
                    injectedValue = runbase.getBean(type);
                }
                JkUtilsReflect.setFieldValue(kbeanUnderInitialization, field, injectedValue);
            }
        }
    }

    private static List<Field> getInjectFields(Class<? extends KBean> kbeanClass, boolean local) {
        List<Field> result = new ArrayList<>();
        for (Field field : JkUtilsReflect.getDeclaredFieldsWithAnnotation(kbeanClass, JkInject.class)) {
            JkInject inject = field.getAnnotation(JkInject.class);
            if (!JkUtilsString.isBlank(inject.value()) == local) {
                continue;  // skip it
            }
            Class<?> type = field.getType();
            if (!local && JkRunbase.class.isAssignableFrom(type)) {
                result.add(field);
            } else if (!KBean.class.isAssignableFrom(type)) {
                throw new JkException(notLBeanClassMsg(field, local));
            } else {
                result.add(field);
            }
        }
        return result;
    }

    private static String notLBeanClassMsg(Field field, boolean local) {
        String orRunbase = local? "" : " or JkRunbase";
        return String.format("Field %s is annotated with @%s but does is not of a KBean type%s. " +
                "%nPlease make the field declare a KBean or remove the annotation.",
                field,
                JkInject.class.getSimpleName(),
                orRunbase);
    }

}
