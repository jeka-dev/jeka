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

import java.lang.annotation.*;

/**
 * Marks a static KBean method to run during KBean initialization.
 * The method must take one parameter: the KBean to pre-initialize.
 * <p>
 * It runs after the KBean is created but before properties or
 * command-line arguments are injected.
 * <p>
 * The method must be static and accept one argument of type KBean
 * or its subclass.
 * <p>
 * To be detected, the method must be within a KBean class involved in
 * the setup phase (a KBean declared with @myKBean= property, present
 * on the command line, or identified as the 'init' bean).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface JkPreInit {

}
