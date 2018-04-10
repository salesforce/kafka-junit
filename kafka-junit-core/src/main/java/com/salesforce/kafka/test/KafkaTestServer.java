/**
 * Copyright (c) 2017-2018, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *   disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.kafka.test;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * This will spin up a ZooKeeper and Kafka server for use in integration tests. Simply
 * create an instance of KafkaTestServer and call start() and you can publish to Kafka
 * topics in an integration test. Be sure to call shutdown() when the test is complete
 * or use the AutoCloseable interface.
 */
public class KafkaTestServer implements AutoCloseable {
    /**
     * Internal Test Zookeeper service.
     */
    private TestingServer zkServer;

    /**
     * Internal Test Kafka service.
     */
    private KafkaServerStartable kafka;

    /**
     * Defines what address the service advertises itself on.
     * Sane default is 127.0.0.1.
     */
    private final String localHostname;

    /**
     * Default constructor using "127.0.0.1" as the advertised host.
     */
    public KafkaTestServer() {
        this("127.0.0.1");
    }

    /**
     * Alternative constructor allowing override of advertised host.
     * @param localHostname What IP or hostname to advertise services on.
     */
    public KafkaTestServer(final String localHostname) {
        this.localHostname = localHostname;
    }

    /**
     * @return Internal Zookeeper Server.
     */
    public TestingServer getZookeeperServer() {
        return this.zkServer;
    }

    /**
     * @return Internal Kafka Server.
     */
    public KafkaServerStartable getKafkaServer() {
        return this.kafka;
    }

    /**
     * @return The proper connect string to use for Kafka.
     */
    public String getKafkaConnectString() {
        return localHostname + ":" + getKafkaServer().serverConfig().advertisedPort();
    }

    /**
     * @return The proper connect string to use for Zookeeper.
     */
    public String getZookeeperConnectString() {
        return localHostname + ":" + getZookeeperServer().getPort();
    }

    /**
     * Creates and starts ZooKeeper and Kafka server instances.
     * @throws Exception on startup errors.
     */
    public void start() throws Exception {
        // Start zookeeper
        final InstanceSpec zkInstanceSpec = new InstanceSpec(null, -1, -1, -1, true, -1, -1, 1000);
        zkServer = new TestingServer(zkInstanceSpec, true);
        final String zkConnectionString = getZookeeperServer().getConnectString();

        // Create temp path to store logs
        final File logDir = Files.createTempDir();
        logDir.deleteOnExit();

        // Determine what port to run kafka on
        final String kafkaPort = String.valueOf(InstanceSpec.getRandomPort());

        // Build properties
        Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("zookeeper.connect", zkConnectionString);
        kafkaProperties.setProperty("port", kafkaPort);
        kafkaProperties.setProperty("log.dir", logDir.getAbsolutePath());
        kafkaProperties.setProperty("auto.create.topics.enable", "true");
        kafkaProperties.setProperty("zookeeper.session.timeout.ms", "30000");
        kafkaProperties.setProperty("broker.id", "1");
        kafkaProperties.setProperty("auto.offset.reset", "latest");

        // Ensure that we're advertising appropriately
        kafkaProperties.setProperty("host.name", localHostname);
        kafkaProperties.setProperty("advertised.host.name", localHostname);
        kafkaProperties.setProperty("advertised.port", kafkaPort);
        kafkaProperties.setProperty("advertised.listeners", "PLAINTEXT://" + localHostname + ":" + kafkaPort);
        kafkaProperties.setProperty("listeners", "PLAINTEXT://" + localHostname + ":" + kafkaPort);

        // Lower active threads.
        kafkaProperties.setProperty("num.io.threads", "2");
        kafkaProperties.setProperty("num.network.threads", "2");
        kafkaProperties.setProperty("log.flush.interval.messages", "1");

        // Define replication factor for internal topics to 1
        kafkaProperties.setProperty("offsets.topic.replication.factor", "1");
        kafkaProperties.setProperty("offset.storage.replication.factor", "1");
        kafkaProperties.setProperty("transaction.state.log.replication.factor", "1");
        kafkaProperties.setProperty("config.storage.replication.factor", "1");
        kafkaProperties.setProperty("status.storage.replication.factor", "1");
        kafkaProperties.setProperty("default.replication.factor", "1");

        final KafkaConfig config = new KafkaConfig(kafkaProperties);
        kafka = new KafkaServerStartable(config);
        getKafkaServer().startup();
    }

    /**
     * Creates a namespace in Kafka. If the namespace already exists this does nothing.
     * Will create a namespace with exactly 1 partition.
     * @param topicName - the namespace name to create.
     */
    public void createTopic(final String topicName) {
        createTopic(topicName, 1);
    }

    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName - the namespace name to create.
     * @param partitions - the number of partitions to create.
     */
    public void createTopic(final String topicName, final int partitions) {
        final short replicationFactor = 1;

        // Create admin client
        try (final AdminClient adminClient = KafkaAdminClient.create(buildDefaultClientConfig())) {
            try {
                // Define topic
                final NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);

                // Create topic, which is async call.
                final CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singleton(newTopic));

                // Since the call is Async, Lets wait for it to complete.
                createTopicsResult.values().get(topicName).get();
            } catch (InterruptedException | ExecutionException e) {
                if (!(e.getCause() instanceof TopicExistsException)) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                // TopicExistsException - Swallow this exception, just means the topic already exists.
            }
        }
    }

    /**
     * Shuts down the ZooKeeper and Kafka server instances. This *must* be called before the integration
     * test completes in order to clean up any running processes and data that was created.
     * @throws Exception on shutdown errors.
     */
    public void shutdown() throws Exception {
        close();
    }

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer) {

        // Build config
        final Map<String, Object> kafkaProducerConfig = Maps.newHashMap();
        kafkaProducerConfig.put("bootstrap.servers", getKafkaConnectString());
        kafkaProducerConfig.put("key.serializer", keySerializer);
        kafkaProducerConfig.put("value.serializer", valueSerializer);
        kafkaProducerConfig.put("max.in.flight.requests.per.connection", 1);
        kafkaProducerConfig.put("retries", 5);
        kafkaProducerConfig.put("client.id", getClass().getSimpleName() + " Producer");
        kafkaProducerConfig.put("batch.size", 0);

        // Create and return Producer.
        return new KafkaProducer<>(kafkaProducerConfig);
    }

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer) {

        // Build config
        Map<String, Object> kafkaConsumerConfig = buildDefaultClientConfig();
        kafkaConsumerConfig.put("key.deserializer", keyDeserializer);
        kafkaConsumerConfig.put("value.deserializer", valueDeserializer);
        kafkaConsumerConfig.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");

        // Create and return Consumer.
        return new KafkaConsumer<>(kafkaConsumerConfig);
    }

    /**
     * Internal helper method to build a default configuration.
     */
    private Map<String, Object> buildDefaultClientConfig() {
        Map<String, Object> defaultClientConfig = Maps.newHashMap();
        defaultClientConfig.put("bootstrap.servers", getKafkaConnectString());
        defaultClientConfig.put("client.id", "test-consumer-id");
        return defaultClientConfig;
    }

    /**
     * Closes the internal servers. Failing to call this at the end of your tests will likely
     * result in leaking instances.
     */
    @Override
    public void close() throws Exception {
        if (getKafkaServer() != null) {
            getKafkaServer().shutdown();
            kafka = null;
        }
        if (getZookeeperServer() != null) {
            getZookeeperServer().close();
            zkServer = null;
        }
    }
}
