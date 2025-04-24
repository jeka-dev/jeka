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
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static dev.jeka.core.tool.KBeanAction.Action.*;

/**
 * Represents an action to be taken on a bean, such as invoking a method,
 * injecting a property, or instantiating a bean.
 */
class KBeanAction implements Comparable<KBeanAction> {

    enum Action {

        SET_FIELD_VALUE("set-value"), INVOKE("invoke"), BEAN_INIT("initialize");

        private final String name;

        Action(String name) {
            this.name = name;
        }
    }

    final Action type;

    final Class<? extends KBean> beanClass;

    final String member; // method or property name

    final Object value; // if property

    final String valueSource;

    private KBeanAction(Action type, Class<? extends KBean> beanClass, String member, Object value, String valueSource) {
        this.type = type;
        this.beanClass = beanClass;
        this.member = member;
        this.value = value;
        this.valueSource = valueSource;
    }

    static KBeanAction ofInitialization(Class<? extends KBean> beanClass) {
        return new KBeanAction(Action.BEAN_INIT, beanClass, null, null, null);
    }

    static KBeanAction ofInvoke(Class<? extends KBean> beanClass, String methodName) {
        return new KBeanAction(Action.INVOKE, beanClass, methodName, null, null);
    }

    static KBeanAction ofSetValue(Class<? extends KBean> beanClass, String fieldName, Object value, String source) {
        return new KBeanAction(Action.SET_FIELD_VALUE, beanClass, fieldName, value, source);
    }

    @Override
    public String toString() {
        if (type == Action.BEAN_INIT) {
            return beanClass.getName() + ".init()";
        }
        if (type == Action.INVOKE) {
            return beanClass.getName() + "." + member + "()";
        }
        return beanClass.getName() + "." + member + "=" + value + "  [from " + valueSource + "]";
    }

    @Override
    public int compareTo(KBeanAction other) {
        if (this.beanClass.equals(other.beanClass)) {
            if (this.type == Action.INVOKE && other.type != Action.INVOKE) {
                return 1;
            }
            if (this.type != Action.INVOKE && other.type == Action.INVOKE) {
                return -1;
            }
            if (this.type == BEAN_INIT) {
                return -1;
            }
            if (other.type == BEAN_INIT) {
                return 1;
            }
        }
        return 0;
    }

    Method method() {
        JkUtilsAssert.state(type == INVOKE, "Can get method only on INVOKE action type, was %s", this);
        return JkUtilsReflect.getMethod(beanClass, member);
    }

    public Comparator<KBeanAction> compareTo() {

        return (c1, c2) -> {
            if (c1.beanClass.equals(c2.beanClass)) {
                if (c1.type == Action.INVOKE && c2.type != Action.INVOKE) {
                    return 1;
                }
                if (c1.type != Action.INVOKE && c2.type == Action.INVOKE) {
                    return -1;
                }
                if (c1.type == BEAN_INIT) {
                    return -1;
                }
                if (c2.type == BEAN_INIT) {
                    return 1;
                }
            }
            return 0;
        };
    }

    static class Container {

        private final List<KBeanAction> kBeanActions = new LinkedList<>();

        List<KBeanAction> toList() {
            return kBeanActions.stream().sorted().collect(Collectors.toList());
        }

        List<KBeanAction> findSetValues(Class<? extends KBean> kbeanClass) {
            return kBeanActions.stream()
                    .filter(action -> action.type == Action.SET_FIELD_VALUE)
                    .filter(action -> action.beanClass.equals(kbeanClass))
                    .collect(Collectors.toList());
        }

        List<KBeanAction> findInvokes() {
            return kBeanActions.stream()
                    .filter(action -> action.type == INVOKE)
                    .collect(Collectors.toList());
        }

        List<Class<? extends KBean>> findInvolvedKBeanClasses() {
            return kBeanActions.stream()
                    .map(action -> action.beanClass)
                    .distinct()
                    .collect(Collectors.toList());
        }

        void addAll(List<KBeanAction> kBeanActions) {
            kBeanActions.forEach(this::add);
        }

        void add(KBeanAction kBeanAction) {

            // Always add invokes
            if (kBeanAction.type == INVOKE) {
                kBeanActions.add(kBeanAction);

            // Add instantiation only if it has not been already done
            } else if (kBeanAction.type == BEAN_INIT) {
                boolean present = kBeanActions.stream()
                        .filter(kBeanAction1 -> kBeanAction1.type == BEAN_INIT)
                        .anyMatch(kBeanAction1 -> kBeanAction.beanClass.equals(kBeanAction1.beanClass));
                if (!present) {
                    kBeanActions.add(kBeanAction);
                }

            // If field inject has already bean declared on same bean/field, it is replaced
            } else if (kBeanAction.type == SET_FIELD_VALUE) {
                KBeanAction present = kBeanActions.stream()
                        .filter(kBeanAction1 -> kBeanAction1.type == SET_FIELD_VALUE)
                        .filter(kBeanAction1 -> kBeanAction.beanClass.equals(kBeanAction1.beanClass))
                        .filter(kBeanAction1 -> kBeanAction.member.equals(kBeanAction1.member))
                        .findFirst().orElse(null);
                if (present == null) {
                    kBeanActions.add(kBeanAction);
                } else {
                    int index = kBeanActions.indexOf(present);
                    kBeanActions.remove(index);
                    kBeanActions.add(index, kBeanAction);
                }
            }
        }

        JkColumnText toColumnText() {
            JkColumnText columnText = JkColumnText.ofSingle(1, 30)  // actionType
                    .addColumn(1, 50)  // member
                    .addColumn(1, 16); // source
            List<KBeanAction> sortedCommands = this.kBeanActions.stream()
                    .sorted().collect(Collectors.toList());
            for (KBeanAction action : sortedCommands) {
                String value = Objects.toString(action.value);
                if (action.type == SET_FIELD_VALUE &&
                        JkProperties.SENSITIVE_KEY_PATTERN.test(action.member)) {
                    value = "***";
                }
                String member = action.type == Action.INVOKE ? action.member + "()" : action.member + "=" + value;
                member = action.type == BEAN_INIT ? "init" : member;

                // Don't show null assignment at log info level
                boolean cancel = !JkLog.isVerbose() && (action.type == SET_FIELD_VALUE && action.value == null);
                if (!cancel) {
                    columnText.add(
                            action.beanClass.getSimpleName(),
                            JkUtilsString.nullToEmpty(member),
                            JkUtilsString.nullToEmpty(action.valueSource)
                    );
                }
            }
            return columnText;
        }

        KBeanAction.Container withKBeanInitialization(Class<? extends KBean> initKBeanClass) {
            if (initKBeanClass == null) {
                return this;
            }
            KBeanAction.Container container = new KBeanAction.Container();
            container.addAll(this.kBeanActions);
            container.addKBeanInitialization(initKBeanClass);
            return container;
        }

        KBeanAction.Container withOnlyKBeanClasses(List<String> kbeanClassNames) {
            List<KBeanAction> kBeanActions = this.kBeanActions.stream()
                    .filter(kBeanAction -> kbeanClassNames.contains(kBeanAction.beanClass.getName()))
                    .collect(Collectors.toList());
            KBeanAction.Container result = new KBeanAction.Container();
            result.addAll(kBeanActions);
            return result;
        }

        KBeanAction.Container withoutAnyOfKBeanClasses(List<String> kbeanClassNames) {
            List<KBeanAction> kBeanActions = this.kBeanActions.stream()
                    .filter(kBeanAction -> !kbeanClassNames.contains(kBeanAction.beanClass.getName()))
                    .collect(Collectors.toList());
            KBeanAction.Container result = new KBeanAction.Container();
            result.addAll(kBeanActions);
            return result;
        }

        private void addKBeanInitialization(Class<? extends KBean> defaultKBeanClass) {
            KBeanAction present = kBeanActions.stream()
                    .filter(action -> action.type == BEAN_INIT)
                    .filter(action -> action.beanClass.equals(defaultKBeanClass))
                    .findFirst().orElse(null);
            if (present != null) {
                kBeanActions.remove(present);
            }
            kBeanActions.add(0, KBeanAction.ofInitialization(defaultKBeanClass));
        }

        String toCmdLineRun() {
            List<String> sortedCommands = this.kBeanActions.stream()
                    .sorted()
                    .filter(kBeanAction -> kBeanAction.type == INVOKE)
                    .map(kBeanAction -> kBeanAction.beanClass.getSimpleName() + "." + kBeanAction.member)
                    .collect(Collectors.toList());
            return String.join(" ", sortedCommands);
        }

        // split in chunks having the same KBean
        List<Container> splitByKBean() {
            List<Class<? extends KBean>> involvedKBeanClasses = kBeanActions.stream()
                    .filter(kBeanAction -> kBeanAction.type == INVOKE)
                    .map(action -> action.beanClass)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Class<? extends KBean>, KBeanAction.Container> involvedKBeanContainers = new HashMap<>();
            LinkedHashMap<Class<? extends KBean>, List<KBeanAction>> map = new LinkedHashMap<>();
            for (KBeanAction action : kBeanActions) {
                Class<? extends KBean> kBeanClass = action.beanClass;
                map.putIfAbsent(kBeanClass, new LinkedList<>());
                map.get(kBeanClass).add(action);
            }
            return map.values().stream()
                    .map(actions -> {
                        Container splittedContainer = new Container();
                        splittedContainer.addAll(actions);
                        return splittedContainer;
                    })
                    .collect(Collectors.toList());
        }

    }
}
