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