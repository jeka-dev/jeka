package dev.jeka.plugins.springboot;


import dev.jeka.core.api.depmanagement.JkModuleId;

/**
 * Some common Spring modules
 * 
 * @author Jerome Angibaud
 */
public final class JkSpringModules {

    public static class Fwk {

        public static final String GROUP = "org.springframework";

        public static final JkModuleId AOP = module("spring-aop");

        public static final JkModuleId ASPECTS = module("spring-aspects");

        public static final JkModuleId BEANS = module("spring-beans");

        public static final JkModuleId CONTEXT = module("spring-context");

        public static final JkModuleId CONTEXT_SUPPORT = module("spring-context-support");

        public static final JkModuleId CORE = module("spring-core");

        public static final JkModuleId EXPRESSION = module("spring-expression");

        public static final JkModuleId INSTRUMENT = module("spring-instrument");

        public static final JkModuleId INSTRUMENT_TOMCAT = module("spring-instrument-tomcat");

        public static final JkModuleId JDBC = module("spring-jdbc");

        public static final JkModuleId JMS = module("spring-jms");

        public static final JkModuleId MESSAGING = module("spring-messaging");

        public static final JkModuleId ORM = module("spring-orm");

        public static final JkModuleId OXM = module("spring-oxm");

        public static final JkModuleId TEST = module("spring-test");

        public static final JkModuleId TX = module("spring-tx");

        public static final JkModuleId WEB = module("spring-web");

        public static final JkModuleId WEBMVC = module("spring-webmvc");

        public static final JkModuleId WEBMVC_PORTLET = module("spring-webmvc-portlet");

        public static final JkModuleId WEBSOCKET = module("spring-websocket");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static class Boot {

        public static final String GROUP = "org.springframework.boot";

        public static final String SPRING_GROUP = "org.springframework";

        public static final JkModuleId SPRING_BOOT = module("spring-boot");

        public static final JkModuleId ACTUATOR = module("spring-boot-actuator");

        public static final JkModuleId AUTOCONFIGURE = module("spring-boot-autoconfigure");

        public static final JkModuleId CONFIGURATION_PROCESSOR = module("spring-boot-configuration-processor");

        public static final JkModuleId DEPENDENCY_TOOLS = module("spring-boot-dependency-tools");

        public static final JkModuleId LOADER = module("spring-boot-loader");

        public static final JkModuleId LOADER_TOOLS = module("spring-boot-loader-tools");

        public static final JkModuleId STARTER = module("spring-boot-starter");

        public static final JkModuleId STARTER_ACTUATOR = module("spring-boot-starter-actuator");

        public static final JkModuleId STARTER_AMQP = module("spring-boot-starter-amqp");

        public static final JkModuleId STARTER_AOP = module("spring-boot-starter-aop");

        public static final JkModuleId STARTER_BATCH = module("spring-boot-starter-batch");

        public static final JkModuleId STARTER_CLOUD_CONNECTORS = module("spring-boot-starter-cloud-connectors");

        public static final JkModuleId STARTER_DATA_ELASTICSEARCH = module("spring-boot-starter-data-elasticsearch");

        public static final JkModuleId STARTER_DATA_GEMFIRE = module("spring-boot-starter-data-gemfire");

        public static final JkModuleId STARTER_DATA_JPA = module("spring-boot-starter-data-jpa");

        public static final JkModuleId STARTER_DATA_MONGODB = module("spring-boot-starter-data-mongodb");

        public static final JkModuleId STARTER_DATA_REST = module("spring-boot-starter-data-rest");

        public static final JkModuleId STARTER_DATA_SOLR = module("spring-boot-starter-data-solr");

        public static final JkModuleId STARTER_FREEMARKER = module("spring-boot-starter-freemarker");

        public static final JkModuleId STARTER_GROOVY_TEMPLATES = module("spring-boot-starter-groovy-templates");

        public static final JkModuleId STARTER_HATEOAS = module("spring-boot-starter-hateoas");

        public static final JkModuleId STARTER_HORNETQ = module("spring-boot-starter-hornetq");

        public static final JkModuleId STARTER_INTEGRATION = module("spring-boot-starter-integration");

        public static final JkModuleId STARTER_JDBC = module("spring-boot-starter-jdbc");

        public static final JkModuleId STARTER_JERSEY = module("spring-boot-starter-jersey");

        public static final JkModuleId STARTER_JETTY = module("spring-boot-starter-jetty");

        public static final JkModuleId STARTER_JTA_ATOMIKOS = module("spring-boot-starter-jta-atomikos");

        public static final JkModuleId STARTER_JTA_BITRONIX = module("spring-boot-starter-jta-bitronix");

        public static final JkModuleId STARTER_LOG4J = module("spring-boot-starter-log4j");

        public static final JkModuleId STARTER_LOG4J2 = module("spring-boot-starter-log4j2");

        public static final JkModuleId STARTER_LOGGING = module("spring-boot-starter-logging");

        public static final JkModuleId STARTER_MAIL = module("spring-boot-starter-mail");

        public static final JkModuleId STARTER_MOBILE = module("spring-boot-starter-mobile");

        public static final JkModuleId STARTER_MUSTACHE = module("spring-boot-starter-mustache");

        public static final JkModuleId STARTER_PARENT = module("spring-boot-starter-parent");

        public static final JkModuleId STARTER_DATA_REDIS = module("spring-boot-starter-data-redis");

        public static final JkModuleId STARTER_REMOTE_SHELL = module("spring-boot-starter-remote-shell");

        public static final JkModuleId STARTER_SECURITY = module("spring-boot-starter-security");

        public static final JkModuleId STARTER_SOCIAL_FACEBOOK = module("spring-boot-starter-social-facebook");

        public static final JkModuleId STARTER_SOCIAL_LINKEDIN = module("spring-boot-starter-social-linkedin");

        public static final JkModuleId STARTER_SOCIAL_TWITTER = module("spring-boot-starter-social-twitter");

        public static final JkModuleId STARTER_TEST = module("spring-boot-starter-test");

        public static final JkModuleId STARTER_THYMELEAF = module("spring-boot-starter-thymeleaf");

        public static final JkModuleId STARTER_TOMCAT = module("spring-boot-starter-tomcat");

        public static final JkModuleId STARTER_UNDERTOW = module("spring-boot-starter-undertow");
        
        public static final JkModuleId STARTER_VALIDATION = module("spring-boot-starter-validation");

        public static final JkModuleId STARTER_VELOCITY = module("spring-boot-starter-velocity");

        public static final JkModuleId STARTER_WEB = module("spring-boot-starter-web");

        public static final JkModuleId STARTER_WEBSOCKET = module("spring-boot-starter-websocket");

        public static final JkModuleId STARTER_WS = module("spring-boot-starter-ws");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static class Integration {

        public static final String GROUP = "org.springframework.integration";

        public static final JkModuleId AMQP = module("spring-integration-amqp");

        public static final JkModuleId CORE = module("spring-integration-core");

        public static final JkModuleId EVENT = module("spring-integration-event");

        public static final JkModuleId FEED = module("spring-integration-feed");

        public static final JkModuleId FILE = module("spring-integration-file");

        public static final JkModuleId FTP = module("spring-integration-ftp");

        public static final JkModuleId GEMFIRE = module("spring-integration-gemfire");

        public static final JkModuleId GROOVY = module("spring-integration-groovy");

        public static final JkModuleId HTTP = module("spring-integration-http");

        public static final JkModuleId IP = module("spring-integration-ip");

        public static final JkModuleId JDBC = module("spring-integration-jdbc");

        public static final JkModuleId JMS = module("spring-integration-jms");

        public static final JkModuleId JMX = module("spring-integration-jmx");

        public static final JkModuleId JPA = module("spring-integration-jpa");

        public static final JkModuleId MAIL = module("spring-integration-mail");

        public static final JkModuleId MONGODB = module("spring-integration-mongodb");

        public static final JkModuleId MQTT = module("spring-integration-mqtt");

        public static final JkModuleId REDIS = module("spring-integration-redis");

        public static final JkModuleId RMI = module("spring-integration-rmi");

        public static final JkModuleId SCRIPTING = module("spring-integration-scripting");

        public static final JkModuleId SECURITY = module("spring-integration-security");

        public static final JkModuleId SFTP = module("spring-integration-sftp");

        public static final JkModuleId STREAM = module("spring-integration-stream");

        public static final JkModuleId SYSLOG = module("spring-integration-syslog");

        public static final JkModuleId TEST = module("spring-integration-test");

        public static final JkModuleId TWITTER = module("spring-integration-twitter");

        public static final JkModuleId WEBSOCKET = module("spring-integration-websocket");

        public static final JkModuleId WS = module("spring-integration-ws");

        public static final JkModuleId XML = module("spring-integration-xml");

        public static final JkModuleId XMPP = module("spring-integration-xmpp");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static class Security {

        public static final String GROUP = "org.springframework.security";

        public static final JkModuleId ACL = module("spring-security-acl");

        public static final JkModuleId ASPECTS = module("spring-security-aspects");

        public static final JkModuleId CAS = module("spring-security-cas");

        public static final JkModuleId CONFIG = module("spring-security-config");

        public static final JkModuleId CORE = module("spring-security-core");

        public static final JkModuleId CRYPTO = module("spring-security-crypto");

        public static final JkModuleId LDAP = module("spring-security-ldap");

        public static final JkModuleId OPENID = module("spring-security-openid");

        public static final JkModuleId REMOTING = module("spring-security-remoting");

        public static final JkModuleId TAGLIBS = module("spring-security-taglibs");

        public static final JkModuleId WEB = module("spring-security-web");

        public static final JkModuleId JWT = module("spring-security-jwt");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static class Amqp {

        public static final String GROUP = "org.springframework.amqp";

        public static final JkModuleId AMQP = module("spring-amqp");

        public static final JkModuleId RABBIT = module("spring-rabbit");

        public static final JkModuleId ERLANG = module("spring-erlang");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static class Batch {

        public static final String GROUP = "org.springframework.batch";

        public static final JkModuleId CORE = module("spring-batch-core");

        public static final JkModuleId INFRASTRUCTURE = module("spring-batch-infrastructure");

        public static final JkModuleId INTEGRATION = module("spring-batch-integration");

        public static final JkModuleId TEST = module("spring-batch-test");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Data {

        public static final String GROUP = "org.springframework.data";

        public static final JkModuleId CASSANDRA = module("spring-data-cassandra");

        public static final JkModuleId CQL = module("spring-cql");

        public static final JkModuleId COMMONS = module("spring-data-commons");

        public static final JkModuleId COUCHBASE = module("spring-data-couchbase");

        public static final JkModuleId ELASTICSEARCH = module("spring-data-elasticsearch");

        public static final JkModuleId GEMFIRE = module("spring-data-gemfire");

        public static final JkModuleId JPA = module("spring-data-jpa");

        public static final JkModuleId MONGODB = module("spring-data-mongodb");

        public static final JkModuleId MONGODB_CROSS_STORE = module("spring-data-mongodb-cross-store");

        public static final JkModuleId MONGODB_LOG4J = module("spring-data-mongodb-log4j");

        public static final JkModuleId NEO4J = module("spring-data-neo4j");

        public static final JkModuleId REDIS = module("spring-data-redis");

        public static final JkModuleId REST_CORE = module("spring-data-rest-core");

        public static final JkModuleId REST_WEBMVC = module("spring-data-rest-webmvc");

        public static final JkModuleId SOLR = module("spring-data-solr");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Hateoas {

        public static final String GROUP = "org.springframework.hateoas";

        public static final JkModuleId HATEOAS = module("spring-hateoas");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Social {

        public static final String GROUP = "org.springframework.social";

        public static final JkModuleId CONFIG = module("spring-social-config");

        public static final JkModuleId CORE = module("spring-social-core");

        public static final JkModuleId FACEBOOK = module("spring-social-facebook");

        public static final JkModuleId FACEBOOK_WEB = module("spring-social-facebook-web");

        public static final JkModuleId LINKEDIN = module("spring-social-linkedin");

        public static final JkModuleId SECURITY = module("spring-social-security");

        public static final JkModuleId TWITTER = module("spring-social-twitter");

        public static final JkModuleId WEB = module("spring-social-web");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Ws {

        public static final String GROUP = "org.springframework.ws";

        public static final JkModuleId CORE = module("spring-ws-core");

        public static final JkModuleId SECURITY = module("spring-ws-security");

        public static final JkModuleId SUPPORT = module("spring-ws-support");

        public static final JkModuleId TEST = module("spring-ws-test");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Mobile {

        public static final String GROUP = "org.springframework.mobile";

        public static final JkModuleId DEVICE = module("spring-mobile-device");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Plugin {

        public static final String GROUP = "org.springframework.plugin";

        public static final JkModuleId CORE = module("spring-plugin-core");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

    public static final class Cloud {

        public static final String GROUP = "org.springframework.cloud";

        public static final JkModuleId CLOUDFOUNDRY_CONNECTOR = module("spring-cloud-cloudfoundry-connector");

        public static final JkModuleId CORE = module("spring-cloud-core");

        public static final JkModuleId HEROKU_CONNECTOR = module("spring-cloud-heroku-connector");

        public static final JkModuleId LOCALCONFIG_CONNECTOR = module("spring-cloud-localconfig-connector");

        public static final JkModuleId SPRING_SERVICE_CONNECTOR = module("spring-cloud-spring-service-connector");

        private static final JkModuleId module(String name) {
            return JkModuleId.of(GROUP, name);
        }

    }

}
