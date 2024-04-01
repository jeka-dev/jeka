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

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.CommandLine.Help.Visibility;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;
import dev.jeka.core.tool.CommandLine.Model.OptionSpec;
import dev.jeka.core.tool.builtins.admin.AdminKBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.ide.EclipseKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.builtins.tooling.nexus.NexusKBean;

import java.util.List;
import java.util.Objects;

class PicocliCommands {

    static final List<Class<? extends KBean>> STANDARD_KBEAN_CLASSES = JkUtilsIterable.listOf(
            AdminKBean.class,
            BaseKBean.class,
            ProjectKBean.class,
            MavenKBean.class,
            GitKBean.class,
            DockerKBean.class,
            IntellijKBean.class,
            EclipseKBean.class,
            NexusKBean.class
    );

    static CommandLine mainCommandLine() {
        PicocliMainCommand mainCommand = new PicocliMainCommand();
        CommandSpec mainCommandSpec = CommandSpec.forAnnotatedObject(mainCommand);
        return new CommandLine(mainCommandSpec);
    }

    static CommandLine stdHelp() {
        CommandSpec commandSpec = CommandSpec.create()
                .mixinStandardHelpOptions(true).helpCommand(true);
        return new CommandLine(commandSpec);
    }

    static CommandSpec fromKBeanDesc(KBeanDescription beanDesc) {
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

    private static CommandSpec fromKBeanMethod(KBeanDescription.BeanMethod beanMethod) {
        CommandSpec spec = CommandSpec.create();
        spec.subcommandsRepeatable(true);
        String description = beanMethod.description == null ? "No Description" : beanMethod.description;
        description = description.endsWith(".") ? description : description + ".";
        spec.usageMessage().header(description);
        return spec;
    }


}
