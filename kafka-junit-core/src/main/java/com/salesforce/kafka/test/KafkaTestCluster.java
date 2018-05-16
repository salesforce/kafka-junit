package com.salesforce.kafka.test;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Utility for setting up a Cluster of KafkaTestServers.
 */
public class KafkaTestCluster implements AutoCloseable {

    private final int numberOfBrokers;
    private final List<KafkaTestServer> brokers = new ArrayList<>();

    /**
     * Internal Test Zookeeper service.
     */
    private TestingServer zkServer;

    /**
     * Constructor.
     * @param numberOfBrokers How many brokers you want in your Kafka cluster.
     */
    public KafkaTestCluster(final int numberOfBrokers) {
        if (numberOfBrokers <= 0) {
            throw new IllegalArgumentException("numberOfBrokers argument must be 1 or larger.");
        }
        this.numberOfBrokers = numberOfBrokers;
    }

    public void start() {
        // Start Zookeeper instance.
        final InstanceSpec zkInstanceSpec = new InstanceSpec(null, -1, -1, -1, true, -1, -1, 1000);
        try {
            zkServer = new TestingServer(zkInstanceSpec, true);
            zkServer.start();
        } catch (Exception e) {
            // Convert to runtime.
            throw new RuntimeException(e.getMessage(), e);
        }
        final String zkConnectionString = zkServer.getConnectString();

        // Loop over brokers
        for (int brokerId = 1; brokerId <= numberOfBrokers; brokerId++) {
            // Create properties for brokers
            final Properties brokerProperties = new Properties();
            brokerProperties.put("broker.id", String.valueOf(brokerId));
            brokerProperties.put("zookeeper.connect", zkConnectionString);

            // Create new KafkaTestServer
            final KafkaTestServer kafkaBroker = new KafkaTestServer(brokerProperties);

            // Start it
            kafkaBroker.start();

            // Add to our broker list
            brokers.add(kafkaBroker);
        }
    }

    public List<KafkaTestServer> getKafkaBrokers() {
        return Collections.unmodifiableList(brokers);
    }

    public KafkaTestServer getKafkaBrokerById(final int brokerId) {
        // Brokers are zero indexed in the array.
        return brokers.get(brokerId - 1);
    }

    @Override
    public void close() {
        // Loop over brokers
        for (final KafkaTestServer kafkaBroker : brokers) {
            kafkaBroker.close();
        }

        // Stop zkServer
        if (zkServer == null) {
            return;
        }
        try {
            zkServer.stop();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
