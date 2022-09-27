package dev.jeka.core.api.depmanagement;

/**
 * Constants on popular modules used in Java ecosystem.
 *
 * @author Jerome Angibaud
 */
public final class JkPopularLibs {

    private JkPopularLibs() {
        // Unused constructor
    }

    public static final JkCoordinate JAVAX_SERVLET_API = JkCoordinate.of("javax.servlet:javax.servlet-api");

    public static final JkCoordinate JAVAX_MAIL_API = JkCoordinate.of("javax.mail:javax.mail-api");

    public static final JkCoordinate JAVAX_CONNECTOR_API = JkCoordinate.of("javax.resource:connector-api");

    public static final JkCoordinate JAVAX_CONNECTOR = JkCoordinate.of("javax.resource:connector");

    public static final JkCoordinate JAVAX_INJECT = JkCoordinate.of("javax.inject:javax.inject");

    public static final JkCoordinate JAVAX_CDI_API = JkCoordinate.of("javax.enterprise:javax.inject");

    public static final JkCoordinate JAVAX_VALIDATION_API = JkCoordinate.of("javax.validation:validation-api");

    public static final JkCoordinate JAVAX_XML_BIND = JkCoordinate.of("javax.xml.bind:jaxb-api");

    public static final JkCoordinate JAVAX_JSP = JkCoordinate.of("javax.servlet.jsp:jsp-api");

    public static final JkCoordinate JAVAX_PERSISTENCE_API = JkCoordinate.of("javax.persistence:persistence-api");

    public static final JkCoordinate JAVAX_JSTL = JkCoordinate.of("javax.servlet:jstl");

    public static final JkCoordinate JAVAX_JSR311_API = JkCoordinate.of("javax.ws.rs:jsr311-api");

    public static final JkCoordinate JAVAX_JSR250_API = JkCoordinate.of("javax.ws.rs:jsr250-api");

    public static final JkCoordinate JAVAX_JTA = JkCoordinate.of("javax.transaction:jta");

    public static final JkCoordinate JAVAX_ACTIVATION = JkCoordinate.of("javax.activation:activation");

    public static final JkCoordinate JAVAX_JAVAEE_API = JkCoordinate.of("javax:avaee-api");

    public static final JkCoordinate JAVAX_JCR = JkCoordinate.of("javax.jcr:jcr");

    public static final JkCoordinate JAVAX_EL = JkCoordinate.of("javax.el:el-api");

    public static final JkCoordinate JAVAX_JDO2 = JkCoordinate.of("javax.jdo:jdo2-api");

    public static final JkCoordinate JAVAX_WEB_SOCKET_SERVER_API = JkCoordinate.of("javax.websocket:javax.websocket-api");

    public static final JkCoordinate JAVAX_JMS = JkCoordinate.of("javax.jms:jms-api");

    public static final JkCoordinate JUNIT = JkCoordinate.of("junit:junit");

    public static final JkCoordinate JUNIT_5 = JkCoordinate.of("org.junit.jupiter:junit-jupiter");

    public static final JkCoordinate JUNIT_5_PLATFORM_LAUNCHER = JkCoordinate.of("org.junit.platform:junit-platform-launcher");

    public static final JkCoordinate GUAVA = JkCoordinate.of("com.google.guava:guava");

    public static final JkCoordinate GUICE = JkCoordinate.of("com.google.inject:guice");

    public static final JkCoordinate GSON = JkCoordinate.of("com.google.code.gson:gson");

    public static final JkCoordinate GOOGLE_PROTOCOL_BUFFER = JkCoordinate.of("com.google.protobuf:protobuf-java");

    public static final JkCoordinate SLF4J_API = JkCoordinate.of("org.slf4j:slf4j-api");

    public static final JkCoordinate SLF4J_SIMPLE_BINDING = JkCoordinate.of("org.slf4j:slf4j-simple");

    public static final JkCoordinate SLF4J_LOG4J12_BINDING = JkCoordinate.of("org.slf4j:slf4j-log4j12");

    public static final JkCoordinate SLF4J_JCL_OVER_SLF4J = JkCoordinate.of("org.slf4j:jcl-over-slf4j");

    public static final JkCoordinate SLF4J_JDK14_BINDING = JkCoordinate.of("org.slf4j:slf4j-jdk14");

    public static final JkCoordinate LOGBACK_CLASSIC = JkCoordinate.of("ch.qos.logback:logback-classicJava");

    public static final JkCoordinate LOGBACK_CORE = JkCoordinate.of("ch.qos.logback:logback-core");

    public static final JkCoordinate LOG4J = JkCoordinate.of("log4j:log4j");

    public static final JkCoordinate SPRING_CORE = JkCoordinate.of("org.springframework:spring-core");

    public static final JkCoordinate SPRING_CONTEXT = JkCoordinate.of("org.springframework:spring-context");

    public static final JkCoordinate SPRING_BEANS = JkCoordinate.of("org.springframework:spring-beans");

    public static final JkCoordinate SPRING_JDBC = JkCoordinate.of("org.springframework:spring-jdbc");

    public static final JkCoordinate SPRING_AOP = JkCoordinate.of("org.springframework:spring-aop");

    public static final JkCoordinate SPRING_TX = JkCoordinate.of("org.springframework:spring-tx");

    public static final JkCoordinate SPRING_ORM = JkCoordinate.of("org.springframework:spring-orm");

    public static final JkCoordinate SPRING_SECURITY_CORE = JkCoordinate.of("org.springframework:spring-security-core");

    public static final JkCoordinate GLIB = JkCoordinate.of("cglib:cglib");

    public static final JkCoordinate JODA_TIME = JkCoordinate.of("joda-time:joda-time");

    public static final JkCoordinate APACHE_HTTP_CLIENT = JkCoordinate.of("org.apache.httpcomponents:httpclient");

    public static final JkCoordinate APACHE_HTTP_CORE = JkCoordinate.of("org.apache.httpcomponents:httpcore");

    public static final JkCoordinate APACHE_COMMONS_DBCP = JkCoordinate.of("commons-dbcp:commons-dbcp");

    public static final JkCoordinate APACHE_COMMONS_NET = JkCoordinate.of("commons-net:commons-net");

    public static final JkCoordinate JACKSON_CORE = JkCoordinate.of("com.fasterxml.jackson.core: jackson-core");

    public static final JkCoordinate JACKSON_DATABIND = JkCoordinate.of("com.fasterxml.jackson.core: jackson-databind");

    public static final JkCoordinate JACKSON_ANNOTATIONS = JkCoordinate.of("com.fasterxml.jackson.core:jackson-annotations");

    public static final JkCoordinate MOCKITO_ALL = JkCoordinate.of("org.mockito:mockito-all");

    public static final JkCoordinate MOCKITO_CORE = JkCoordinate.of("org.mockito:mockito-core");

    public static final JkCoordinate HIBERNATE_CORE = JkCoordinate.of("org.hibernate:hibernate-core");

    public static final JkCoordinate HIBERNATE_JPA_SUPPORT = JkCoordinate.of("org.hibernate:hibernate-entitymanager");

    public static final JkCoordinate HIBERNATE_VALIDATOR = JkCoordinate.of("org.hibernate:hibernate-validator");

    public static final JkCoordinate JETTY_SERVER = JkCoordinate.of("org.eclipse.jetty:jetty-server");

    public static final JkCoordinate JERSEY_CORE = JkCoordinate.of("com.sun.jersey:jersey-core");

    public static final JkCoordinate JERSEY_SERVER = JkCoordinate.of("com.sun.jersey:jersey-server");

    public static final JkCoordinate MYSQL_CONNECTOR = JkCoordinate.of("mysql:jmysql-connector-java");

    public static final JkCoordinate EHCACHE_CORE = JkCoordinate.of("net.fs.ehcache:ehcache-core");

    public static final JkCoordinate EHCACHE = JkCoordinate.of("net.fs.ehcache:ehcache");

    public static final JkCoordinate SELENIUM_JAVA = JkCoordinate.of("org.seleniumhq.selenium:selenium-java");

    public static final JkCoordinate METRICS_CORE = JkCoordinate.of("io.dropwizard.metrics:metrics-core");

    public static final JkCoordinate METRICS_JVM = JkCoordinate.of("io.dropwizard.metrics:metrics-jvm");

    public static final JkCoordinate METRICS_ANNOTATION = JkCoordinate.of("io.dropwizard.metrics:metrics-annotation");

    public static final JkCoordinate BOUNCY_CASTLE_PROVIDER = JkCoordinate.of("org.bouncycastle:bcprov-jdk16");

    public static final JkCoordinate MX4J = JkCoordinate.of("mx4j:mx4j");

    public static final JkCoordinate JGIT = JkCoordinate.of("org.eclipse.jgit:org.eclipse.jgit");

    public static final JkCoordinate SVN_KIT = JkCoordinate.of("org.tmatesoft.svnkit:svnkit");

    public static final JkCoordinate LOMBOK = JkCoordinate.of("org.projectlombok:lombok");

    public static final JkCoordinate H2 = JkCoordinate.of("com.h2database:h2");
}
