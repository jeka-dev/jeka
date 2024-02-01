package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gives the description of a KBean : its name, its purpose and its base class.
 *
 * @author Jerome Angibaud
 */
// TODO maybe to merge with KBeanDescription
final class KBeanDoc implements Comparable<KBeanDoc> {

    private static String longName(Class<?> clazz) {
        return clazz.getName();
    }

    private final String shortName;

    private final String fullName;

    private final Class<? extends KBean> clazz;

    KBeanDoc(Class<? extends KBean> clazz) {
        super();
        this.shortName = KBean.name(clazz);
        this.fullName = longName(clazz);
        this.clazz = clazz;
    }

    public List<String> pluginDependencies() {
        return JkUtilsReflect.getAllDeclaredFields(clazz, false).stream()
                .filter(field -> KBean.class.isAssignableFrom(field.getType()))
                .map(Field::getType)
                .map(Class::getName)
                .collect(Collectors.toList());
    }

    public String shortName() {
        return this.shortName;
    }

    public String fullName() {
        return this.fullName;
    }

    public Class<? extends KBean> beanClass() {
        return clazz;
    }

    public List<String> description() {
        if (this.clazz.getAnnotation(JkDoc.class) == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(this.clazz.getAnnotation(JkDoc.class).value());
    }

    public String shortDescription() {
        List<String> description = description();
        if (description.isEmpty()) {
            return null;
        }
        String result = description.get(0);
        return result.contains("\n") ?  JkUtilsString.substringBeforeFirst(result, "\n") : result;
    }

    public List<String> activationEffect() {
        JkDoc doc = JkUtilsReflect.getInheritedAnnotation(clazz, JkDoc.class, "activate");
        return doc == null ? Collections.emptyList() : Arrays.asList(doc.value());
    }

    @Override
    public String toString() {
        return "name=" + this.shortName + "(" + this.fullName + ")";
    }

    @Override
    public int compareTo(KBeanDoc o) {
        return this.shortName.compareTo(o.shortName);
    }
}
