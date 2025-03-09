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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds an entry to the jeka-src classpath where this annotation is declared.
 * Typically, the annotated element is a <code>KBean</code> class from 'jeka/def' source directory.
 * But when used on a public field of type  <code>KBean</code> within a <code>KBean</code> class,
 * the annotated field is injected with an initialised <code>KBean</code> instance of the imported project.<p>
 *
 * Example :
 * <pre><code>
 * public class FatJarBuild extends KBean {
 *
 *     @JkInject
 *     ProjectKBean projectKBean;
 *
 *     @JkInject("../anotherJekaProject")
 *     private AClassicBuild sampleBuild;
 *
 *     ...
 * </code></pre>
 *
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface JkInject {

    /** Dependee project relative path */
    String value() default "";

}
