package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.JkCoordinate.GroupAndName;

/**
 * Constants on popular modules used in Java ecosystem.
 *
 * @author Jerome Angibaud
 */
public final class JkPopularLibs {

    private JkPopularLibs() {
        // Unused constructor
    }

    public static final GroupAndName JAVAX_SERVLET_API = GroupAndName.of("javax.servlet:javax.servlet-api");

    public static final GroupAndName JAVAX_MAIL_API = GroupAndName.of("javax.mail:javax.mail-api");

    public static final GroupAndName JAVAX_CONNECTOR_API = GroupAndName.of("javax.resource:connector-api");

    public static final GroupAndName JAVAX_CONNECTOR = GroupAndName.of("javax.resource:connector");

    public static final GroupAndName JAVAX_INJECT = GroupAndName.of("javax.inject:javax.inject");

    public static final GroupAndName JAVAX_CDI_API = GroupAndName.of("javax.enterprise:javax.inject");

    public static final GroupAndName JAVAX_VALIDATION_API = GroupAndName.of("javax.validation:validation-api");

    public static final GroupAndName JAVAX_XML_BIND = GroupAndName.of("javax.xml.bind:jaxb-api");

    public static final GroupAndName JAVAX_JSP = GroupAndName.of("javax.servlet.jsp:jsp-api");

    public static final GroupAndName JAVAX_PERSISTENCE_API = GroupAndName.of("javax.persistence:persistence-api");

    public static final GroupAndName JAVAX_JSTL = GroupAndName.of("javax.servlet:jstl");

    public static final GroupAndName JAVAX_JSR311_API = GroupAndName.of("javax.ws.rs:jsr311-api");

    public static final GroupAndName JAVAX_JSR250_API = GroupAndName.of("javax.ws.rs:jsr250-api");

    public static final GroupAndName JAVAX_JTA = GroupAndName.of("javax.transaction:jta");

    public static final GroupAndName JAVAX_ACTIVATION = GroupAndName.of("javax.activation:activation");

    public static final GroupAndName JAVAX_JAVAEE_API = GroupAndName.of("javax:avaee-api");

    public static final GroupAndName JAVAX_JCR = GroupAndName.of("javax.jcr:jcr");

    public static final GroupAndName JAVAX_EL = GroupAndName.of("javax.el:el-api");

    public static final GroupAndName JAVAX_JDO2 = GroupAndName.of("javax.jdo:jdo2-api");

    public static final GroupAndName JAVAX_WEB_SOCKET_SERVER_API = GroupAndName.of("javax.websocket:javax.websocket-api");

    public static final GroupAndName JAVAX_JMS = GroupAndName.of("javax.jms:jms-api");

    public static final GroupAndName JUNIT = GroupAndName.of("junit:junit");

    public static final GroupAndName JUNIT_5 = GroupAndName.of("org.junit.jupiter:junit-jupiter");

    public static final GroupAndName JUNIT_5_PLATFORM_LAUNCHER = GroupAndName.of("org.junit.platform:junit-platform-launcher");

    public static final GroupAndName GUAVA = GroupAndName.of("com.google.guava:guava");

    public static final GroupAndName GUICE = GroupAndName.of("com.google.inject:guice");

    public static final GroupAndName GSON = GroupAndName.of("com.google.code.gson:gson");

    public static final GroupAndName GOOGLE_PROTOCOL_BUFFER = GroupAndName.of("com.google.protobuf:protobuf-java");

    public static final GroupAndName SLF4J_API = GroupAndName.of("org.slf4j:slf4j-api");

    public static final GroupAndName SLF4J_SIMPLE_BINDING = GroupAndName.of("org.slf4j:slf4j-simple");

    public static final GroupAndName SLF4J_LOG4J12_BINDING = GroupAndName.of("org.slf4j:slf4j-log4j12");

    public static final GroupAndName SLF4J_JCL_OVER_SLF4J = GroupAndName.of("org.slf4j:jcl-over-slf4j");

    public static final GroupAndName SLF4J_JDK14_BINDING = GroupAndName.of("org.slf4j:slf4j-jdk14");

    public static final GroupAndName LOGBACK_CLASSIC = GroupAndName.of("ch.qos.logback:logback-classicJava");

    public static final GroupAndName LOGBACK_CORE = GroupAndName.of("ch.qos.logback:logback-core");

    public static final GroupAndName LOG4J = GroupAndName.of("log4j:log4j");

    public static final GroupAndName SPRING_CORE = GroupAndName.of("org.springframework:spring-core");

    public static final GroupAndName SPRING_CONTEXT = GroupAndName.of("org.springframework:spring-context");

    public static final GroupAndName SPRING_BEANS = GroupAndName.of("org.springframework:spring-beans");

    public static final GroupAndName SPRING_JDBC = GroupAndName.of("org.springframework:spring-jdbc");

    public static final GroupAndName SPRING_AOP = GroupAndName.of("org.springframework:spring-aop");

    public static final GroupAndName SPRING_TX = GroupAndName.of("org.springframework:spring-tx");

    public static final GroupAndName SPRING_ORM = GroupAndName.of("org.springframework:spring-orm");

    public static final GroupAndName SPRING_SECURITY_CORE = GroupAndName.of("org.springframework:spring-security-core");

    public static final GroupAndName GLIB = GroupAndName.of("cglib:cglib");

    public static final GroupAndName JODA_TIME = GroupAndName.of("joda-time:joda-time");

    public static final GroupAndName APACHE_HTTP_CLIENT = GroupAndName.of("org.apache.httpcomponents:httpclient");

    public static final GroupAndName APACHE_HTTP_CORE = GroupAndName.of("org.apache.httpcomponents:httpcore");

    public static final GroupAndName APACHE_COMMONS_DBCP = GroupAndName.of("commons-dbcp:commons-dbcp");

    public static final GroupAndName APACHE_COMMONS_NET = GroupAndName.of("commons-net:commons-net");

    public static final GroupAndName JACKSON_CORE = GroupAndName.of("com.fasterxml.jackson.core: jackson-core");

    public static final GroupAndName JACKSON_DATABIND = GroupAndName.of("com.fasterxml.jackson.core: jackson-databind");

    public static final GroupAndName JACKSON_ANNOTATIONS = GroupAndName.of("com.fasterxml.jackson.core:jackson-annotations");

    public static final GroupAndName MOCKITO_ALL = GroupAndName.of("org.mockito:mockito-all");

    public static final GroupAndName MOCKITO_CORE = GroupAndName.of("org.mockito:mockito-core");

    public static final GroupAndName HIBERNATE_CORE = GroupAndName.of("org.hibernate:hibernate-core");

    public static final GroupAndName HIBERNATE_JPA_SUPPORT = GroupAndName.of("org.hibernate:hibernate-entitymanager");

    public static final GroupAndName HIBERNATE_VALIDATOR = GroupAndName.of("org.hibernate:hibernate-validator");

    public static final GroupAndName JETTY_SERVER = GroupAndName.of("org.eclipse.jetty:jetty-server");

    public static final GroupAndName JERSEY_CORE = GroupAndName.of("com.sun.jersey:jersey-core");

    public static final GroupAndName JERSEY_SERVER = GroupAndName.of("com.sun.jersey:jersey-server");

    public static final GroupAndName MYSQL_CONNECTOR = GroupAndName.of("mysql:jmysql-connector-java");

    public static final GroupAndName EHCACHE_CORE = GroupAndName.of("net.fs.ehcache:ehcache-core");

    public static final GroupAndName EHCACHE = GroupAndName.of("net.fs.ehcache:ehcache");

    public static final GroupAndName SELENIUM_JAVA = GroupAndName.of("org.seleniumhq.selenium:selenium-java");

    public static final GroupAndName METRICS_CORE = GroupAndName.of("io.dropwizard.metrics:metrics-core");

    public static final GroupAndName METRICS_JVM = GroupAndName.of("io.dropwizard.metrics:metrics-jvm");

    public static final GroupAndName METRICS_ANNOTATION = GroupAndName.of("io.dropwizard.metrics:metrics-annotation");

    public static final GroupAndName BOUNCY_CASTLE_PROVIDER = GroupAndName.of("org.bouncycastle:bcprov-jdk16");

    public static final GroupAndName MX4J = GroupAndName.of("mx4j:mx4j");

    public static final GroupAndName JGIT = GroupAndName.of("org.eclipse.jgit:org.eclipse.jgit");

    public static final GroupAndName SVN_KIT = GroupAndName.of("org.tmatesoft.svnkit:svnkit");

    public static final GroupAndName LOMBOK = GroupAndName.of("org.projectlombok:lombok");

    public static final GroupAndName H2 = GroupAndName.of("com.h2database:h2");
}
