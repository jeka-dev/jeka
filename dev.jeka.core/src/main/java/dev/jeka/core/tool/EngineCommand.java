package dev.jeka.core.tool;

class EngineCommand {

    enum Action {
        PROPERTY_INJECT, METHOD_INVOKE, BEAN_REGISTRATION
    }

    private Action action;

    private Class<? extends JkBean> beanClass;

    private String member;

    private Object value;  // for properties only

    EngineCommand(Action action, Class<? extends JkBean> beanClass, String valueOrMethod, Object value) {
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

    public Object getValue() {
        return value;
    }

}
