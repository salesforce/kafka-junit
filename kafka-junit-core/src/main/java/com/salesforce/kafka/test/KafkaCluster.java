package com.salesforce.kafka.test;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Properties;

public interface KafkaCluster extends AutoCloseable {

    /**
     * Creates and starts ZooKeeper and Kafka server instances.
     * @throws Exception on startup errors.
     */
    void start() throws Exception;

    /**
     * Closes the internal servers. Failing to call this at the end of your tests will likely
     * result in leaking instances.
     */
    void close() throws Exception;

    /**
     * @return The proper connect string to use for Kafka.
     */
    String getKafkaConnectString();

    /**
     * @return The proper connect string to use for Zookeeper.
     */
    String getZookeeperConnectString();

    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName the namespace name to create.
     * @param partitions the number of partitions to create.
     * @param replicationFactor the number of replicas for the topic.
     */
    void createTopic(final String topicName, final int partitions, final short replicationFactor);

    /**
     * Creates a Kafka AdminClient connected to our test cluster.
     * @return Kafka AdminClient instance.
     */
    AdminClient getAdminClient();

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer
    );

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @param config Additional producer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer,
        final Properties config
    );

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer
    );

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @param config Additional consumer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer,
        final Properties config
    );
}
