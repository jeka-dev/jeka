package dev.jeka.core.tool;

/**
 * Represents an action to be taken on a bean, such as invoking a method,
 * injecting a property, or instantiating a bean.
 */
class KBeanAction {

    final EngineCommand.Action action;

    final String beanName;

    final String member; // method or property name

    final Object value; // if property

    public KBeanAction(EngineCommand.Action action, String beanName, String member, Object value) {
        this.action = action;
        this.beanName = beanName;
        this.member = member;
        this.value = value;
    }

    @Override
    public String toString() {
        return "action=" + action + ", beanName='" + beanName + '\'' + ", member='" + member + '\'' +
                ", value='" + value + '\'';
    }

}
