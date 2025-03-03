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

import dev.jeka.core.tool.CommandLine.Help.Visibility;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;
import dev.jeka.core.tool.CommandLine.Model.OptionSpec;

import java.util.Objects;

class PicocliCommands {

    static CommandLine mainCommandLine() {
        PicocliMainCommand mainCommand = new PicocliMainCommand();
        CommandSpec mainCommandSpec = CommandSpec.forAnnotatedObject(mainCommand);
        return new CommandLine(mainCommandSpec);
    }

    static CommandSpec fromKBeanDesc(JkBeanDescription beanDesc) {
        CommandSpec spec = CommandSpec.create();
        beanDesc.beanFields.forEach(beanField -> {

            Visibility showDefault = beanDesc.includeDefaultValues ?
                    Visibility.ALWAYS : Visibility.NEVER;


            String defaultValue = beanField.defaultValue == null  ? CommandLine.Option.NULL_VALUE
                    : Objects.toString(beanField.defaultValue);

            String description = beanField.description == null ?
                    "No description." : beanField.description;
            description = description.trim();
            description = description.endsWith(".") ? description : description + ".";

            String customDefaultDesc = "";
            if (beanField.injectedPropertyName != null ) {
                showDefault = Visibility.NEVER;
                String initialDefaultValue = defaultValue;
                defaultValue ="${" + beanField.injectedPropertyName + ":-" + defaultValue + "}";
                customDefaultDesc = "\n  Default: {" + beanField.injectedPropertyName + ":-" +initialDefaultValue + "}";
            }

            String acceptedValues = beanField.type.isEnum() ?
                    " [${COMPLETION-CANDIDATES}]" : "";
            String[] descriptions = (description + acceptedValues + customDefaultDesc).split("\n");
            OptionSpec optionSpec = OptionSpec.builder(beanField.name)
                    .description(descriptions)
                    .type(beanField.type)
                    .showDefaultValue(showDefault)
                    .hideParamSyntax(false)
                    .defaultValue(defaultValue)
                    .paramLabel("<" + beanField.type.getSimpleName() + ">")
                    .build();
            spec.addOption(optionSpec);
        });
        beanDesc.beanMethods.forEach(beanMethod -> {
            spec.addSubcommand(beanMethod.name, fromKBeanMethod(beanMethod));
        });
        return spec;
    }

    private static CommandSpec fromKBeanMethod(JkBeanDescription.BeanMethod beanMethod) {
        CommandSpec spec = CommandSpec.create();
        spec.subcommandsRepeatable(true);
        String description = beanMethod.description == null ? "No Description" : beanMethod.description;
        description = description.endsWith(".") ? description : description + ".";
        spec.usageMessage().header(description);
        return spec;
    }


}
