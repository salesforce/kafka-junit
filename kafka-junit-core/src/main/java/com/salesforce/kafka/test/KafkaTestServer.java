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

import com.google.common.io.Files;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.File;
import java.util.Properties;

/**
 * This will spin up a ZooKeeper and Kafka server for use in integration tests. Simply
 * create an instance of KafkaTestServer and call start() and you can publish to Kafka
 * topics in an integration test. Be sure to call shutdown() when the test is complete
 * or use the AutoCloseable interface.
 */
public class KafkaTestServer implements KafkaCluster, KafkaProvider {
    /**
     * This defines the hostname the kafka instance will listen on by default.
     */
    private static final String DEFAULT_HOSTNAME = "127.0.0.1";

    /**
     * Internal Test Zookeeper service.
     */
    private TestingServer zkServer;

    /**
     * Flag to know if we are managing the zookeeper server.
     */
    private boolean isManagingZookeeper = true;

    /**
     * Internal Test Kafka service.
     */
    private KafkaServerStartable kafka;

    /**
     * Random Generated Kafka Port.
     */
    private String kafkaPort;

    /**
     * Defines overridden broker properties.
     */
    private final Properties overrideBrokerProperties = new Properties();

    /**
     * Default constructor.
     */
    public KafkaTestServer() {
        this(new Properties());
    }

    /**
     * Alternative constructor allowing override of advertised host.
     * @param localHostname What IP or hostname to advertise services on.
     * @deprecated Replaced with constructor: KafkaTestServer(final Properties overrideBrokerProperties)
     *             Set "host.name" property to the hostname you want kafka to listen on.
     */
    @Deprecated
    public KafkaTestServer(final String localHostname) {
        this(new Properties());

        // Configure passed in hostname in broker properties.
        overrideBrokerProperties.put("host.name", localHostname);
    }

    /**
     * Alternative constructor allowing override of brokerProperties.
     * @param overrideBrokerProperties Define Kafka broker properties.
     */
    public KafkaTestServer(final Properties overrideBrokerProperties) {
        // Validate argument.
        if (overrideBrokerProperties == null) {
            throw new IllegalArgumentException("Cannot pass null overrideBrokerProperties argument");
        }

        // Add passed in properties.
        this.overrideBrokerProperties.putAll(overrideBrokerProperties);
    }

    /**
     * Package protected constructor allowing override of ZookeeperTestServer instance.
     * @param overrideBrokerProperties Define Kafka broker properties.
     * @param zookeeperTestServer Zookeeper server instance to use.
     */
    KafkaTestServer(final Properties overrideBrokerProperties, final TestingServer zookeeperTestServer) {
        this(overrideBrokerProperties);

        // If instance is passed,
        if (zookeeperTestServer != null) {
            // We are no longer in charge of managing it.
            isManagingZookeeper = false;
        }
        // Save reference.
        this.zkServer = zookeeperTestServer;
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
        return getConfiguredHostname() + ":" + kafkaPort;
    }

    /**
     * @return The proper connect string to use for Zookeeper.
     */
    public String getZookeeperConnectString() {
        return getConfiguredHostname() + ":" + getZookeeperServer().getPort();
    }

    /**
     * Creates and starts ZooKeeper and Kafka server instances.
     * @throws Exception on startup errors.
     */
    public void start() throws Exception {
        // If we have no zkServer instance
        if (zkServer == null) {
            // Create it.
            final InstanceSpec zkInstanceSpec = new InstanceSpec(null, -1, -1, -1, true, -1, -1, 1000);
            zkServer = new TestingServer(zkInstanceSpec, false);
        }

        // Start zookeeper and get its connection string.
        zkServer.start();
        final String zkConnectionString = getZookeeperServer().getConnectString();

        // Build properties using a baseline from overrideBrokerProperties.
        final Properties brokerProperties = new Properties();
        brokerProperties.putAll(overrideBrokerProperties);

        // Put required zookeeper connection properties.
        setPropertyIfNotSet(brokerProperties, "zookeeper.connect", zkConnectionString);

        // Conditionally generate a port for kafka to use if not already defined.
        kafkaPort = (String) setPropertyIfNotSet(brokerProperties, "port", String.valueOf(InstanceSpec.getRandomPort()));

        // If log.dir is not set.
        if (brokerProperties.getProperty("log.dir") == null) {
            // Create temp path to store logs
            final File logDir = Files.createTempDir();
            logDir.deleteOnExit();

            // Set property.
            brokerProperties.setProperty("log.dir", logDir.getAbsolutePath());
        }

        // Ensure that we're advertising appropriately
        setPropertyIfNotSet(brokerProperties, "host.name", getConfiguredHostname());
        setPropertyIfNotSet(brokerProperties, "advertised.host.name", getConfiguredHostname());
        setPropertyIfNotSet(brokerProperties, "advertised.port", kafkaPort);
        setPropertyIfNotSet(brokerProperties, "advertised.listeners", "PLAINTEXT://" + getConfiguredHostname() + ":" + kafkaPort);
        setPropertyIfNotSet(brokerProperties, "listeners", "PLAINTEXT://" + getConfiguredHostname() + ":" + kafkaPort);

        // Set other defaults if not defined.
        setPropertyIfNotSet(brokerProperties, "auto.create.topics.enable", "true");
        setPropertyIfNotSet(brokerProperties, "zookeeper.session.timeout.ms", "30000");
        setPropertyIfNotSet(brokerProperties, "broker.id", "1");
        setPropertyIfNotSet(brokerProperties, "auto.offset.reset", "latest");

        // Lower active threads.
        setPropertyIfNotSet(brokerProperties, "num.io.threads", "2");
        setPropertyIfNotSet(brokerProperties, "num.network.threads", "2");
        setPropertyIfNotSet(brokerProperties, "log.flush.interval.messages", "1");

        // Define replication factor for internal topics to 1
        setPropertyIfNotSet(brokerProperties, "offsets.topic.replication.factor", "1");
        setPropertyIfNotSet(brokerProperties, "offset.storage.replication.factor", "1");
        setPropertyIfNotSet(brokerProperties, "transaction.state.log.replication.factor", "1");
        setPropertyIfNotSet(brokerProperties, "transaction.state.log.min.isr", "1");
        setPropertyIfNotSet(brokerProperties, "transaction.state.log.num.partitions", "4");
        setPropertyIfNotSet(brokerProperties, "config.storage.replication.factor", "1");
        setPropertyIfNotSet(brokerProperties, "status.storage.replication.factor", "1");
        setPropertyIfNotSet(brokerProperties, "default.replication.factor", "1");

        // Create and start kafka service.
        final KafkaConfig config = new KafkaConfig(brokerProperties);
        kafka = new KafkaServerStartable(config);
        getKafkaServer().startup();
    }

    /**
     * Creates a namespace in Kafka. If the namespace already exists this does nothing.
     * Will create a namespace with exactly 1 partition.
     * @param topicName the namespace name to create.
     */
    public void createTopic(final String topicName) {
        createTopic(topicName, 1);
    }

    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName the namespace name to create.
     * @param partitions the number of partitions to create.
     */
    public void createTopic(final String topicName, final int partitions) {
        createTopic(topicName, partitions, (short) 1);
    }

    @Override
    public void createTopic(final String topicName, final int partitions, final short replicationFactor) {
        new KafkaTestUtils(this)
            .createTopic(topicName, partitions, replicationFactor);
    }

    /**
     * Shuts down the ZooKeeper and Kafka server instances. This *must* be called before the integration
     * test completes in order to clean up any running processes and data that was created.
     * @throws Exception on shutdown errors.
     * @deprecated see close().
     */
    @Deprecated
    public void shutdown() throws Exception {
        close();
    }

    /**
     * Creates a Kafka AdminClient connected to our test server.
     * @return Kafka AdminClient instance.
     */
    public AdminClient getAdminClient() {
        return new KafkaTestUtils(this)
            .getAdminClient();
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

        return getKafkaProducer(keySerializer, valueSerializer, new Properties());
    }

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @param config Additional producer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer,
        final Properties config) {

        return new KafkaTestUtils(this)
            .getKafkaProducer(keySerializer, valueSerializer, config);
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
        return getKafkaConsumer(keyDeserializer, valueDeserializer, new Properties());
    }

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @param config Additional consumer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer,
        final Properties config) {

        return new KafkaTestUtils(this)
            .getKafkaConsumer(keyDeserializer, valueDeserializer, config);
    }

    /**
     * Helper method to conditionally set a property if no value is already set.
     * @param properties The properties instance to update.
     * @param key The key to set if not already set.
     * @param defaultValue The value to set if no value is already set for key.
     * @return The value set.
     */
    private Object setPropertyIfNotSet(final Properties properties, final String key, final String defaultValue) {
        // Validate inputs
        if (properties == null) {
            throw new NullPointerException("properties argument cannot be null.");
        }
        if (key == null) {
            throw new NullPointerException("key argument cannot be null.");
        }

        // Conditionally set the property if its not already set.
        properties.setProperty(
            key,
            properties.getProperty(key, defaultValue)
        );

        // Return the value that is being used.
        return properties.get(key);
    }

    /**
     * Returns which hostname/IP address Kafka will bind/listen/advertise with.  To change this value
     * use the constructor: KafkaTestServer(final Properties overrideBrokerProperties) and set the property
     * 'host.name' to the appropriate value.
     *
     * @return Which hostname/IP Kafka should bind/listen/advertise using.
     */
    private String getConfiguredHostname() {
        return overrideBrokerProperties.getProperty("host.name", DEFAULT_HOSTNAME);
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

        // Conditionally close zookeeper
        if (getZookeeperServer() != null && isManagingZookeeper) {
            // Only close the zkServer if we're in charge of managing it.
            getZookeeperServer().close();
        }
    }
}