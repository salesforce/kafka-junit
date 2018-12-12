package com.salesforce.kafka.test.junit4;

import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.listeners.SaslSslListener;
import org.junit.ClassRule;

/**
 * NOTE: This test case assumes you've started the JVM with the argument.
 *
 * -Djava.security.auth.login.config=kafka-junit-core/src/test/resources/jaas.conf
 *
 * and run the script/generateCertificatesForTest.sh script.
 *
 * Runs smoke tests against an SASL+SSL enabled cluster.
 * @see AbstractSharedKafkaTestResourceTest for test case definitions.
 */
public class SharedKafkaTestResourceWithSaslSslTest extends AbstractSharedKafkaTestResourceTest {
    /**
     * We have a two node kafka cluster that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     *
     * This example we start a cluster with
     *  - 2 brokers (defaults to a single broker)
     *  - configure the brokers to disable topic auto-creation.
     *  - Enable SASL_SSL authentication, using username 'kafkaclient' and password 'client-secret'
     */
    @ClassRule
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        // Start a cluster with 2 brokers.
        .withBrokers(2)
        // Disable topic auto-creation.
        .withBrokerProperty("auto.create.topics.enable", "false")
        // Register and configure SASL PLAIN authentication on cluster.
        .registerListener(new SaslSslListener()
            .withUsername("kafkaclient")
            .withPassword("client-secret")
            .withClientAuthRequested()
            .withKeyStoreLocation(SharedKafkaTestResourceWithSslTest.class.getClassLoader().getResource("kafka.keystore.jks").getFile())
            .withKeyStorePassword("password")
            .withTrustStoreLocation(SharedKafkaTestResourceWithSslTest.class.getClassLoader().getResource("kafka.truststore.jks").getFile())
            .withTrustStorePassword("password")
            .withKeyPassword("password")
        );

    /**
     * Simple accessor.
     */
    protected KafkaTestUtils getKafkaTestUtils() {
        return sharedKafkaTestResource.getKafkaTestUtils();
    }
}
