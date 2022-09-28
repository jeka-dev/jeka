package dev.jeka.plugins.springboot;


import dev.jeka.core.api.depmanagement.JkCoordinate.GroupAndName;

/**
 * Some common Spring modules
 * 
 * @author Jerome Angibaud
 */
public final class JkSpringModules {

    public static class Fwk {

        public static final String GROUP = "org.springframework";

        public static final GroupAndName AOP = groupAndName("spring-aop");

        public static final GroupAndName ASPECTS = groupAndName("spring-aspects");

        public static final GroupAndName BEANS = groupAndName("spring-beans");

        public static final GroupAndName CONTEXT = groupAndName("spring-context");

        public static final GroupAndName CONTEXT_SUPPORT = groupAndName("spring-context-support");

        public static final GroupAndName CORE = groupAndName("spring-core");

        public static final GroupAndName EXPRESSION = groupAndName("spring-expression");

        public static final GroupAndName INSTRUMENT = groupAndName("spring-instrument");

        public static final GroupAndName INSTRUMENT_TOMCAT = groupAndName("spring-instrument-tomcat");

        public static final GroupAndName JDBC = groupAndName("spring-jdbc");

        public static final GroupAndName JMS = groupAndName("spring-jms");

        public static final GroupAndName MESSAGING = groupAndName("spring-messaging");

        public static final GroupAndName ORM = groupAndName("spring-orm");

        public static final GroupAndName OXM = groupAndName("spring-oxm");

        public static final GroupAndName TEST = groupAndName("spring-test");

        public static final GroupAndName TX = groupAndName("spring-tx");

        public static final GroupAndName WEB = groupAndName("spring-web");

        public static final GroupAndName WEBMVC = groupAndName("spring-webmvc");

        public static final GroupAndName WEBMVC_PORTLET = groupAndName("spring-webmvc-portlet");

        public static final GroupAndName WEBSOCKET = groupAndName("spring-websocket");

        private static final GroupAndName groupAndName(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static class Boot {

        public static final String GROUP = "org.springframework.boot";

        public static final String SPRING_GROUP = "org.springframework";

        public static final GroupAndName SPRING_BOOT = module("spring-boot");

        public static final GroupAndName ACTUATOR = module("spring-boot-actuator");

        public static final GroupAndName AUTOCONFIGURE = module("spring-boot-autoconfigure");

        public static final GroupAndName CONFIGURATION_PROCESSOR = module("spring-boot-configuration-processor");

        public static final GroupAndName DEPENDENCY_TOOLS = module("spring-boot-dependency-tools");

        public static final GroupAndName LOADER = module("spring-boot-loader");

        public static final GroupAndName LOADER_TOOLS = module("spring-boot-loader-tools");

        public static final GroupAndName STARTER = module("spring-boot-starter");

        public static final GroupAndName STARTER_ACTUATOR = module("spring-boot-starter-actuator");

        public static final GroupAndName STARTER_AMQP = module("spring-boot-starter-amqp");

        public static final GroupAndName STARTER_AOP = module("spring-boot-starter-aop");

        public static final GroupAndName STARTER_BATCH = module("spring-boot-starter-batch");

        public static final GroupAndName STARTER_CLOUD_CONNECTORS = module("spring-boot-starter-cloud-connectors");

        public static final GroupAndName STARTER_DATA_ELASTICSEARCH = module("spring-boot-starter-data-elasticsearch");

        public static final GroupAndName STARTER_DATA_GEMFIRE = module("spring-boot-starter-data-gemfire");

        public static final GroupAndName STARTER_DATA_JPA = module("spring-boot-starter-data-jpa");

        public static final GroupAndName STARTER_DATA_MONGODB = module("spring-boot-starter-data-mongodb");

        public static final GroupAndName STARTER_DATA_REST = module("spring-boot-starter-data-rest");

        public static final GroupAndName STARTER_DATA_SOLR = module("spring-boot-starter-data-solr");

        public static final GroupAndName STARTER_FREEMARKER = module("spring-boot-starter-freemarker");

        public static final GroupAndName STARTER_GROOVY_TEMPLATES = module("spring-boot-starter-groovy-templates");

        public static final GroupAndName STARTER_HATEOAS = module("spring-boot-starter-hateoas");

        public static final GroupAndName STARTER_HORNETQ = module("spring-boot-starter-hornetq");

        public static final GroupAndName STARTER_INTEGRATION = module("spring-boot-starter-integration");

        public static final GroupAndName STARTER_JDBC = module("spring-boot-starter-jdbc");

        public static final GroupAndName STARTER_JERSEY = module("spring-boot-starter-jersey");

        public static final GroupAndName STARTER_JETTY = module("spring-boot-starter-jetty");

        public static final GroupAndName STARTER_JTA_ATOMIKOS = module("spring-boot-starter-jta-atomikos");

        public static final GroupAndName STARTER_JTA_BITRONIX = module("spring-boot-starter-jta-bitronix");

        public static final GroupAndName STARTER_LOG4J = module("spring-boot-starter-log4j");

        public static final GroupAndName STARTER_LOG4J2 = module("spring-boot-starter-log4j2");

        public static final GroupAndName STARTER_LOGGING = module("spring-boot-starter-logging");

        public static final GroupAndName STARTER_MAIL = module("spring-boot-starter-mail");

        public static final GroupAndName STARTER_MOBILE = module("spring-boot-starter-mobile");

        public static final GroupAndName STARTER_MUSTACHE = module("spring-boot-starter-mustache");

        public static final GroupAndName STARTER_PARENT = module("spring-boot-starter-parent");

        public static final GroupAndName STARTER_DATA_REDIS = module("spring-boot-starter-data-redis");

        public static final GroupAndName STARTER_REMOTE_SHELL = module("spring-boot-starter-remote-shell");

        public static final GroupAndName STARTER_SECURITY = module("spring-boot-starter-security");

        public static final GroupAndName STARTER_SOCIAL_FACEBOOK = module("spring-boot-starter-social-facebook");

        public static final GroupAndName STARTER_SOCIAL_LINKEDIN = module("spring-boot-starter-social-linkedin");

        public static final GroupAndName STARTER_SOCIAL_TWITTER = module("spring-boot-starter-social-twitter");

        public static final GroupAndName STARTER_TEST = module("spring-boot-starter-test");

        public static final GroupAndName STARTER_THYMELEAF = module("spring-boot-starter-thymeleaf");

        public static final GroupAndName STARTER_TOMCAT = module("spring-boot-starter-tomcat");

        public static final GroupAndName STARTER_UNDERTOW = module("spring-boot-starter-undertow");
        
        public static final GroupAndName STARTER_VALIDATION = module("spring-boot-starter-validation");

        public static final GroupAndName STARTER_VELOCITY = module("spring-boot-starter-velocity");

        public static final GroupAndName STARTER_WEB = module("spring-boot-starter-web");

        public static final GroupAndName STARTER_WEBSOCKET = module("spring-boot-starter-websocket");

        public static final GroupAndName STARTER_WS = module("spring-boot-starter-ws");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static class Integration {

        public static final String GROUP = "org.springframework.integration";

        public static final GroupAndName AMQP = module("spring-integration-amqp");

        public static final GroupAndName CORE = module("spring-integration-core");

        public static final GroupAndName EVENT = module("spring-integration-event");

        public static final GroupAndName FEED = module("spring-integration-feed");

        public static final GroupAndName FILE = module("spring-integration-file");

        public static final GroupAndName FTP = module("spring-integration-ftp");

        public static final GroupAndName GEMFIRE = module("spring-integration-gemfire");

        public static final GroupAndName GROOVY = module("spring-integration-groovy");

        public static final GroupAndName HTTP = module("spring-integration-http");

        public static final GroupAndName IP = module("spring-integration-ip");

        public static final GroupAndName JDBC = module("spring-integration-jdbc");

        public static final GroupAndName JMS = module("spring-integration-jms");

        public static final GroupAndName JMX = module("spring-integration-jmx");

        public static final GroupAndName JPA = module("spring-integration-jpa");

        public static final GroupAndName MAIL = module("spring-integration-mail");

        public static final GroupAndName MONGODB = module("spring-integration-mongodb");

        public static final GroupAndName MQTT = module("spring-integration-mqtt");

        public static final GroupAndName REDIS = module("spring-integration-redis");

        public static final GroupAndName RMI = module("spring-integration-rmi");

        public static final GroupAndName SCRIPTING = module("spring-integration-scripting");

        public static final GroupAndName SECURITY = module("spring-integration-security");

        public static final GroupAndName SFTP = module("spring-integration-sftp");

        public static final GroupAndName STREAM = module("spring-integration-stream");

        public static final GroupAndName SYSLOG = module("spring-integration-syslog");

        public static final GroupAndName TEST = module("spring-integration-test");

        public static final GroupAndName TWITTER = module("spring-integration-twitter");

        public static final GroupAndName WEBSOCKET = module("spring-integration-websocket");

        public static final GroupAndName WS = module("spring-integration-ws");

        public static final GroupAndName XML = module("spring-integration-xml");

        public static final GroupAndName XMPP = module("spring-integration-xmpp");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static class Security {

        public static final String GROUP = "org.springframework.security";

        public static final GroupAndName ACL = module("spring-security-acl");

        public static final GroupAndName ASPECTS = module("spring-security-aspects");

        public static final GroupAndName CAS = module("spring-security-cas");

        public static final GroupAndName CONFIG = module("spring-security-config");

        public static final GroupAndName CORE = module("spring-security-core");

        public static final GroupAndName CRYPTO = module("spring-security-crypto");

        public static final GroupAndName LDAP = module("spring-security-ldap");

        public static final GroupAndName OPENID = module("spring-security-openid");

        public static final GroupAndName REMOTING = module("spring-security-remoting");

        public static final GroupAndName TAGLIBS = module("spring-security-taglibs");

        public static final GroupAndName WEB = module("spring-security-web");

        public static final GroupAndName JWT = module("spring-security-jwt");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static class Amqp {

        public static final String GROUP = "org.springframework.amqp";

        public static final GroupAndName AMQP = module("spring-amqp");

        public static final GroupAndName RABBIT = module("spring-rabbit");

        public static final GroupAndName ERLANG = module("spring-erlang");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static class Batch {

        public static final String GROUP = "org.springframework.batch";

        public static final GroupAndName CORE = module("spring-batch-core");

        public static final GroupAndName INFRASTRUCTURE = module("spring-batch-infrastructure");

        public static final GroupAndName INTEGRATION = module("spring-batch-integration");

        public static final GroupAndName TEST = module("spring-batch-test");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Data {

        public static final String GROUP = "org.springframework.data";

        public static final GroupAndName CASSANDRA = module("spring-data-cassandra");

        public static final GroupAndName CQL = module("spring-cql");

        public static final GroupAndName COMMONS = module("spring-data-commons");

        public static final GroupAndName COUCHBASE = module("spring-data-couchbase");

        public static final GroupAndName ELASTICSEARCH = module("spring-data-elasticsearch");

        public static final GroupAndName GEMFIRE = module("spring-data-gemfire");

        public static final GroupAndName JPA = module("spring-data-jpa");

        public static final GroupAndName MONGODB = module("spring-data-mongodb");

        public static final GroupAndName MONGODB_CROSS_STORE = module("spring-data-mongodb-cross-store");

        public static final GroupAndName MONGODB_LOG4J = module("spring-data-mongodb-log4j");

        public static final GroupAndName NEO4J = module("spring-data-neo4j");

        public static final GroupAndName REDIS = module("spring-data-redis");

        public static final GroupAndName REST_CORE = module("spring-data-rest-core");

        public static final GroupAndName REST_WEBMVC = module("spring-data-rest-webmvc");

        public static final GroupAndName SOLR = module("spring-data-solr");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Hateoas {

        public static final String GROUP = "org.springframework.hateoas";

        public static final GroupAndName HATEOAS = module("spring-hateoas");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Social {

        public static final String GROUP = "org.springframework.social";

        public static final GroupAndName CONFIG = module("spring-social-config");

        public static final GroupAndName CORE = module("spring-social-core");

        public static final GroupAndName FACEBOOK = module("spring-social-facebook");

        public static final GroupAndName FACEBOOK_WEB = module("spring-social-facebook-web");

        public static final GroupAndName LINKEDIN = module("spring-social-linkedin");

        public static final GroupAndName SECURITY = module("spring-social-security");

        public static final GroupAndName TWITTER = module("spring-social-twitter");

        public static final GroupAndName WEB = module("spring-social-web");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Ws {

        public static final String GROUP = "org.springframework.ws";

        public static final GroupAndName CORE = module("spring-ws-core");

        public static final GroupAndName SECURITY = module("spring-ws-security");

        public static final GroupAndName SUPPORT = module("spring-ws-support");

        public static final GroupAndName TEST = module("spring-ws-test");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Mobile {

        public static final String GROUP = "org.springframework.mobile";

        public static final GroupAndName DEVICE = module("spring-mobile-device");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Plugin {

        public static final String GROUP = "org.springframework.plugin";

        public static final GroupAndName CORE = module("spring-plugin-core");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

    public static final class Cloud {

        public static final String GROUP = "org.springframework.cloud";

        public static final GroupAndName CLOUDFOUNDRY_CONNECTOR = module("spring-cloud-cloudfoundry-connector");

        public static final GroupAndName CORE = module("spring-cloud-core");

        public static final GroupAndName HEROKU_CONNECTOR = module("spring-cloud-heroku-connector");

        public static final GroupAndName LOCALCONFIG_CONNECTOR = module("spring-cloud-localconfig-connector");

        public static final GroupAndName SPRING_SERVICE_CONNECTOR = module("spring-cloud-spring-service-connector");

        private static final GroupAndName module(String name) {
            return GroupAndName.of(GROUP, name);
        }

    }

}
