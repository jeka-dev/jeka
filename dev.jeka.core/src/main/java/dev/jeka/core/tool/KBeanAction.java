package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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

    static KBeanAction ofInit(Class<? extends KBean> beanClass) {
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

        void addAll(List<KBeanAction> kBeanActions) {
            kBeanActions.forEach(this::add);
        }

        void addInitBean(Class<? extends KBean> inintKBeanClass) {
            KBeanAction present = kBeanActions.stream()
                            .filter(action -> action.type == BEAN_INIT)
                            .filter(action -> action.beanClass.equals(inintKBeanClass))
                            .findFirst().orElse(null);
            if (present != null) {
                kBeanActions.remove(present);
            }
            kBeanActions.add(0, KBeanAction.ofInit(inintKBeanClass));
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
                columnText.add(
                        action.beanClass.getSimpleName(),
                        JkUtilsString.nullToEmpty(member),
                        JkUtilsString.nullToEmpty(action.valueSource)
                );
            }
            return columnText;
        }


    }
}
