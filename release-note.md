# 0.10.29
- Polish Spring-Boot package
- Add JkProject#clean method

# 0.10.28
- Polish Spring-Boot KBean
- Rename JkGitProcess to JkGit

# 0.10.27
- Polish Git KBean

# 0.10.26
- Intellij Iml generation : minor bug fix
- SpringBoot plugin : upgrade default version to 3.1.4

# 0.10.25
- Polish repo api

# 0.10.23
- Take global.properties in account for defining repos downloading core dependencies
- upgrade classgraph version
- Java : Fix Java compiler was not taken in account file exclusions.
- Dependency management : Clean JkRepo api

# 0.10.22
- Springboot : No jars are generated when 'createXxxJar' are all set to 'false' 

# 0.10.21 
- Add dependencies autocompletion hints for external tools
- Deprecate ProjectJkBean#configure in favor of ProjectJkBean#lately

# 0.10.20
- Improve springboot plugin README.MD
- Fix JkExternalToolApi#getCmdShortcutsProperties 
- Add JkDependencySet#withLocalExclusionsOn method

# 0.10.19
- Includes 'jeka.cmd._appendXXXX=' properties to add extra cmd line arguments
- Add optional information on @JkDepSuggest annotation

# 0.10.18
- Make Maven/Gradle works gracefully in conjunction with JeKa when working in IntelliJ  (documentation in FAQ).
- Improve console output. 

# 0.10.17
- Fix JkProperties injection on nested objects.
- Improve console output.

# 0.10.16
- Introduce #methodName and #fieldName= syntax in command line to replace 'jeka methodName fieldName='.
- Add JkPathTree#watch and JkPathTreeSet#wath methods for watching filesystem.
- Add JkPathTree#checksum and JkPathTreeSet#checksum methods.
- Add Kubernetes showcase in Springboot sample project.

# 0.10.15
- Improve help output
- Add example use-case involving springboot+docker+kubernetes in springboot sample project

# 0.10.14
- Springboot plugin : fix fat-jar generation by re-including boot-loader classes.

# 0.10.13
- Fix JkProperties bug introduced in 0.10.12
- Sanitize default download repositories

# 0.10.12
- Allow to customize repo url for bootstrapping Ivy
- Allow to use system property to redefine local repo location

# 0.10.11
- Fix Springboot plugin to generate example code at scaffold time.

# 0.10.10
- Rework JkSourceGenerator
- Include ProtoBuuffers plugin in mono-repo

# 0.10.9
- Bugfix : re-enable ProjectJkBean scaffold configuration.
- Move JkProjectFlatFace#useSimpleStyle to JkProjectFlatFace#setLayoutStyle.
- Move JkIml#getComponent() to JkIml#component
- Rename JkProject#prodComilation to JkProject#compilation
- Rename JkProject#testing.testCompilation to JkProject#testing.compilation
- Add JkProjectFlatFacade#setMainArtifactJarType convenient method.
- Add ProjectJkBean#cleanPack convenience method.
- Add JkProjectFlatFacade#addSourceGenerator convenience method.

# 0.10.6 
- Bugfix : @JkInjectClasspath(file) was resolved on the working dir and not on the root dir of the project.

# 0.10.5 
- Let users set arbitrary headers on requests towards http repositories
- Remove parent-chaining pattern 

# 0.10.4
- Improve Kotlin integration
- Improve startup performance

# 0.10.3
- Springboot : bootable jar is created quicker
- Springboot : SpringbootJkBean#projectBean() has been replaced by SpringbootJkBean#projectBean
- Kotlin : Add KBean to configure Kotlin JVM projects (experimental)

# 0.10.2
- Jeka shell scripts now take 'jeka.java.version' property in account to select the right Jeka JDK
- Springboot : scaffold generates a README.md file.
- Fix small bugs on Windows
- Fix various typo

# 0.10.1
- Rename JkBean#getImportedJkBeans to JkBean#getImportedBeans
- Fix Sonarqube plugin : Jeka properties 'sonar.xxx' are now taken in account.

# 0.10.0
- Initialize the 0.10.x series !