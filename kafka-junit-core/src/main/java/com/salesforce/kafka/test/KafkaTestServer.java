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

import java.io.File;
import java.util.Collections;
import java.util.Properties;

/**
 * This will spin up a ZooKeeper and Kafka server for use in integration tests. Simply
 * create an instance of KafkaTestServer and call start() and you can publish to Kafka
 * topics in an integration test. Be sure to call shutdown() when the test is complete
 * or use the AutoCloseable interface.
 */
public class KafkaTestServer implements KafkaCluster, KafkaProvider, AutoCloseable {
    /**
     * This defines the hostname the kafka instance will listen on by default.
     */
    private static final String DEFAULT_HOSTNAME = "127.0.0.1";

    /**
     * Internal Test Kafka service.
     */
    private KafkaServerStartable kafka;

    /**
     * Random Generated Kafka Port to listen on.
     */
    private int kafkaPort;

    /**
     * Internal Test Zookeeper service.
     */
    private TestingServer zkServer;

    /**
     * Flag to know if we are managing the zookeeper server.
     */
    private boolean isManagingZookeeper = true;

    /**
     * Defines overridden broker properties.
     */
    private final Properties overrideBrokerProperties = new Properties();

    /**
     * Default constructor, no overridden broker properties.
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
     * Constructor allowing override of ZookeeperTestServer instance.
     * @param overrideBrokerProperties Define Kafka broker properties.
     * @param zookeeperTestServer Zookeeper server instance to use.
     */
    public KafkaTestServer(final Properties overrideBrokerProperties, final TestingServer zookeeperTestServer) {
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
     * @return The proper connect string to use for Kafka.
     */
    @Override
    public String getKafkaConnectString() {
        return getConfiguredHostname() + ":" + kafkaPort;
    }

    /**
     * @return immutable list of hosts for brokers within the cluster.
     */
    @Override
    public KafkaBrokerList getKafkaBrokers() {
        return new KafkaBrokerList(
            Collections.singletonList(new KafkaBroker(getBrokerId(), getConfiguredHostname(), kafkaPort))
        );
    }

    public int getBrokerId() {
        // TODO Only if started.
        return kafka.serverConfig().brokerId();
    }

    /**
     * @return The proper connect string to use for Zookeeper.
     */
    public String getZookeeperConnectString() {
        return getConfiguredHostname() + ":" + zkServer.getPort();
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
        final String zkConnectionString = zkServer.getConnectString();

        // Build properties using a baseline from overrideBrokerProperties.
        final Properties brokerProperties = new Properties();
        brokerProperties.putAll(overrideBrokerProperties);

        // Put required zookeeper connection properties.
        setPropertyIfNotSet(brokerProperties, "zookeeper.connect", zkConnectionString);

        // Conditionally generate a port for kafka to use if not already defined.
        kafkaPort = Integer.parseInt(
            (String) setPropertyIfNotSet(brokerProperties, "port", String.valueOf(InstanceSpec.getRandomPort()))
        );

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
        setPropertyIfNotSet(brokerProperties, "advertised.port", String.valueOf(kafkaPort));
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
        kafka.startup();
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
        if (kafka != null) {
            kafka.shutdown();
            kafka = null;
        }

        // Conditionally close zookeeper
        if (zkServer != null && isManagingZookeeper) {
            // Only close the zkServer if we're in charge of managing it.
            zkServer.close();
        }
    }
}