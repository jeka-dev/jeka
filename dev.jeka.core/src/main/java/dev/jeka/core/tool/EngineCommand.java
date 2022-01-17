package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsAssert;

class EngineCommand {

    enum Action {
        PROPERTY_INJECT, METHOD_INVOKE, BEAN_INSTANTIATION
    }

    private Action action;

    private Class<? extends JkBean> beanClass;

    private String member;

    private String value;  // for properties only

    EngineCommand(Action action, Class<? extends JkBean> beanClass, String valueOrMethod, String value) {
        JkUtilsAssert.argument(beanClass != null, "KBean class cannot be null.");
        this.action = action;
        this.beanClass = beanClass;
        this.member = valueOrMethod;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public Class<? extends JkBean> getBeanClass() {
        return beanClass;
    }

    public String getMember() {
        return member;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EngineCommand{" +
                "action=" + action +
                ", beanClass=" + beanClass +
                ", member='" + member + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
