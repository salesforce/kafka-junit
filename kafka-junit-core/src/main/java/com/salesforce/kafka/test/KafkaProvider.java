package com.salesforce.kafka.test;

public interface KafkaProvider {

    /**
     * @return The proper connect string to use for Kafka.
     */
    String getKafkaConnectString();

    /**
     * @return The proper connect string to use for Zookeeper.
     */
    String getZookeeperConnectString();
}
