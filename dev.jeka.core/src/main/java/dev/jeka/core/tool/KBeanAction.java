package dev.jeka.core.tool;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.tool.EngineCommand.Action.*;

/**
 * Represents an action to be taken on a bean, such as invoking a method,
 * injecting a property, or instantiating a bean.
 */
class KBeanAction {

    final EngineCommand.Action action;

    final String beanName;

    final String member; // method or property name

    final Object value; // if property

    final String valueSource;

    private KBeanAction(EngineCommand.Action action, String beanName, String member, Object value, String valueSource) {
        this.action = action;
        this.beanName = beanName;
        this.member = member;
        this.value = value;
        this.valueSource = valueSource;
    }

    static KBeanAction ofInit(String beanName) {
        return new KBeanAction(EngineCommand.Action.BEAN_INIT, beanName, null, null, null);
    }

    static KBeanAction ofInvoke(String beanName, String methodName) {
        return new KBeanAction(EngineCommand.Action.INVOKE, beanName, methodName, null, null);
    }

    static KBeanAction ofSetValue(String beanName, String fieldName, Object value, String source) {
        return new KBeanAction(EngineCommand.Action.SET_FIELD_VALUE, beanName, fieldName, value, source);
    }

    @Override
    public String toString() {
        return "action=" + action + ", beanName='" + beanName + '\'' + ", member='" + member + '\'' +
                ", value='" + value + '\'' + ", source=" + valueSource;
    }

    static class Container {

        private final List<KBeanAction> kBeanActions = new LinkedList<>();

        List<KBeanAction> toList() {
            return Collections.unmodifiableList(kBeanActions);
        }

        void addAll(List<KBeanAction> kBeanActions) {
            kBeanActions.forEach(this::add);
        }

        void add(KBeanAction kBeanAction) {

            // Always add invokes
            if (kBeanAction.action == INVOKE) {
                kBeanActions.add(kBeanAction);

            // Add instantiation only if it has not been already done
            } else if (kBeanAction.action == BEAN_INIT) {
                boolean present = kBeanActions.stream()
                        .filter(kBeanAction1 -> kBeanAction1.action == BEAN_INIT)
                        .anyMatch(kBeanAction1 -> kBeanAction.beanName.equals(kBeanAction1.beanName));
                if (!present) {
                    kBeanActions.add(kBeanAction);
                }

            // If field inject has already bean declared on same bean/field, it is replaced
            } else if (kBeanAction.action == SET_FIELD_VALUE) {
                KBeanAction present = kBeanActions.stream()
                        .filter(kBeanAction1 -> kBeanAction1.action == SET_FIELD_VALUE)
                        .filter(kBeanAction1 -> kBeanAction.beanName.equals(kBeanAction1.beanName))
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
    }
}
