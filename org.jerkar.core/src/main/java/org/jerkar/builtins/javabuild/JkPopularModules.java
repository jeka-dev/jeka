package org.jerkar.builtins.javabuild;

import org.jerkar.depmanagement.JkModuleId;

public final class JkPopularModules {

	private JkPopularModules() {
		// Unused constructor
	}

	public static final JkModuleId JAVAX_SERVLET_API = JkModuleId.of("javax.servlet", "servlet-api");

	public static final JkModuleId JAVAX_MAIL_API = JkModuleId.of("javax.mail", "javax.mail-api");

	public static final JkModuleId JAVAX_CONNECTOR_API = JkModuleId.of("javax.resource", "connector-api");

	public static final JkModuleId JAVAX_CONNECTOR = JkModuleId.of("javax.resource", "connector");

	public static final JkModuleId JAVAX_INJECT = JkModuleId.of("javax.inject", "javax.inject");

	public static final JkModuleId JAVAX_CDI_API = JkModuleId.of("javax.enterprise", "javax.inject");

	public static final JkModuleId JAVAX_VALIDATION_API = JkModuleId.of("javax.validation", "validation-api");

	public static final JkModuleId JAVAX_XML_BIND = JkModuleId.of("javax.xml.bind", "jaxb-api");

	public static final JkModuleId JAVAX_JSP = JkModuleId.of("javax.servlet.jsp", "jsp-api");

	public static final JkModuleId JAVAX_PERSISTENCE_API = JkModuleId.of("javax.persistence", "persistence-api");

	public static final JkModuleId JAVAX_JSTL = JkModuleId.of("javax.servlet", "jstl");

	public static final JkModuleId JAVAX_JSR311_API = JkModuleId.of("javax.ws.rs", "jsr311-api");

	public static final JkModuleId JAVAX_JSR250_API = JkModuleId.of("javax.ws.rs", "jsr250-api");

	public static final JkModuleId JAVAX_JTA = JkModuleId.of("javax.transaction", "jta");

	public static final JkModuleId JAVAX_ACTIVATION = JkModuleId.of("javax.activation", "activation");

	public static final JkModuleId JAVAX_JAVAEE_API = JkModuleId.of("javax", "javaee-api");

	public static final JkModuleId JAVAX_JCR = JkModuleId.of("javax.jcr", "jcr");

	public static final JkModuleId JAVAX_EL = JkModuleId.of("javax.el", "el-api");

	public static final JkModuleId JAVAX_JDO2 = JkModuleId.of("javax.jdo", "jdo2-api");

	public static final JkModuleId JAVAX_WEB_SOCKET_SERVER_API = JkModuleId.of("javax.websocket", "javax.websocket-api");

	public static final JkModuleId JAVAX_JMS = JkModuleId.of("javax.jms", "jms-api");

	public static final JkModuleId JUNIT = JkModuleId.of("junit", "junit");

	public static final JkModuleId GUAVA = JkModuleId.of("com.google.guava", "guava");

	public static final JkModuleId GUICE = JkModuleId.of("com.google.inject", "guice");

	public static final JkModuleId GSON = JkModuleId.of("com.google.code.gson", "gson");

	public static final JkModuleId GOOGLE_PROTOCOL_BUFFER= JkModuleId.of("com.google.protobuf", "protobuf-java");

	public static final JkModuleId SLF4J_API = JkModuleId.of("org.slf4j", "slf4j-api");

	public static final JkModuleId SLF4J_SIMPLE_BINDING = JkModuleId.of("org.slf4j", "slf4j-simple");

	public static final JkModuleId SLF4J_LOG4J12_BINDING = JkModuleId.of("org.slf4j", "slf4j-log4j12");

	public static final JkModuleId SLF4J_JCL_OVER_SLF4J = JkModuleId.of("org.slf4j", "jcl-over-slf4j");

	public static final JkModuleId SLF4J_JDK14_BINDING = JkModuleId.of("org.slf4j", "slf4j-jdk14");

	public static final JkModuleId LOGBACK_CLASSIC = JkModuleId.of("ch.qos.logback", "logback-classic");

	public static final JkModuleId LOGBACK_CORE = JkModuleId.of("ch.qos.logback", "logback-core");

	public static final JkModuleId LOG4J = JkModuleId.of("log4j", "log4j");

	public static final JkModuleId SPRING_CORE = JkModuleId.of("org.springframework", "spring-core");

	public static final JkModuleId SPRING_CONTEXT = JkModuleId.of("org.springframework", "spring-context");

	public static final JkModuleId SPRING_BEANS = JkModuleId.of("org.springframework", "spring-beans");

	public static final JkModuleId SPRING_JDBC = JkModuleId.of("org.springframework", "spring-jdbc");

	public static final JkModuleId SPRING_AOP = JkModuleId.of("org.springframework", "spring-aop");

	public static final JkModuleId SPRING_TX = JkModuleId.of("org.springframework", "spring-tx");

	public static final JkModuleId SPRING_ORM = JkModuleId.of("org.springframework", "spring-orm");

	public static final JkModuleId SPRING_SECURITY_CORE = JkModuleId.of("org.springframework", "spring-security-core");

	public static final JkModuleId GLIB = JkModuleId.of("cglib", "cglib");

	public static final JkModuleId JERKAR_CORE = JkModuleId.of("org.jerkar", "core");

	public static final JkModuleId JODA_TIME = JkModuleId.of("joda-time", "joda-time");

	public static final JkModuleId APACHE_HTTP_CLIENT = JkModuleId.of("org.apache.httpcomponents", "httpclient");

	public static final JkModuleId APACHE_HTTP_CORE = JkModuleId.of("org.apache.httpcomponents", "httpcore");

	public static final JkModuleId APACHE_COMMONS_DBCP = JkModuleId.of("commons-dbcp", "commons-dbcp");

	public static final JkModuleId APACHE_COMMONS_NET = JkModuleId.of("commons-net", "commons-net");

	public static final JkModuleId JACKSON_CORE = JkModuleId.of("com.fasterxml.jackson.core", " jackson-core");

	public static final JkModuleId JACKSON_DATABIND = JkModuleId.of("com.fasterxml.jackson.core", " jackson-databind");

	public static final JkModuleId JACKSON_ANNOTATIONS = JkModuleId.of("com.fasterxml.jackson.core", " jackson-annotations");

	public static final JkModuleId MOCKITO = JkModuleId.of("org.mockito", "org.mockito");

	public static final JkModuleId HIBERNATE_CORE = JkModuleId.of("org.hibernate", "hibernate-core");

	public static final JkModuleId HIBERNATE_JPA_SUPPORT = JkModuleId.of("org.hibernate", "hibernate-entitymanager");

	public static final JkModuleId HIBERNATE_VALIDATOR = JkModuleId.of("org.hibernate", "hibernate-validator");

	public static final JkModuleId JETTY_SERVER = JkModuleId.of("org.eclipse.jetty", "jetty-server");

	public static final JkModuleId JERSEY_CORE = JkModuleId.of("com.sun.jersey", "jersey-core");

	public static final JkModuleId JERSEY_SERVER = JkModuleId.of("com.sun.jersey", "jersey-server");

	public static final JkModuleId MYSQL_CONNECTOR = JkModuleId.of("mysql", "jmysql-connector-java");

	public static final JkModuleId EHCACHE_CORE = JkModuleId.of("net.fs.ehcache", "ehcache-core");

	public static final JkModuleId EHCACHE = JkModuleId.of("net.fs.ehcache", "ehcache");

	public static final JkModuleId SELENIUM_JAVA = JkModuleId.of("org.seleniumhq.selenium", "selenium-java");

	public static final JkModuleId METRICS_CORE = JkModuleId.of("io.dropwizard.metrics", "metrics-core");

	public static final JkModuleId METRICS_JVM = JkModuleId.of("io.dropwizard.metrics", "metrics-jvm");

	public static final JkModuleId METRICS_ANNOTATION = JkModuleId.of("io.dropwizard.metrics", "metrics-annotation");

	public static final JkModuleId BOUNCY_CASTLE_PROVIDER = JkModuleId.of("org.bouncycastle", "bcprov-jdk16");

	public static final JkModuleId MX4J = JkModuleId.of("mx4j", "mx4j");

	public static final JkModuleId JGIT = JkModuleId.of("org.eclipse.jgit", "org.eclipse.jgit");

	public static final JkModuleId SVN_KIT = JkModuleId.of("org.tmatesoft.svnkit", "svnkit");


}
