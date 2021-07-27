/**
 * Copyright (c) 2017-2020, Salesforce.com, Inc.
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

import com.salesforce.kafka.test.listeners.BrokerListener;
import com.salesforce.kafka.test.listeners.PlainListener;
import kafka.metrics.KafkaMetricsReporter;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import org.apache.kafka.common.utils.Time;
import scala.Option;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This will spin up a ZooKeeper and Kafka server for use in integration tests. Simply
 * create an instance of KafkaTestServer and call start() and you can publish to Kafka
 * topics in an integration test. Be sure to call shutdown() when the test is complete
 * or use the AutoCloseable interface.
 */
public class KafkaTestServer implements KafkaCluster, KafkaProvider, AutoCloseable {
    /**
     * This defines the hostname the kafka instance will listen on by default.
     * To alter the value used, set broker override property 'host.name'
     */
    private static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * Internal Test Kafka service.
     */
    private KafkaServer broker;

    /**
     * Holds the broker configuration.
     */
    private KafkaConfig brokerConfig;

    /**
     * Internal Test Zookeeper service.
     */
    private ZookeeperTestServer zookeeperTestServer;

    /**
     * Flag to know if we are managing the zookeeper server.
     */
    private boolean isManagingZookeeper = true;

    /**
     * Defines overridden broker properties.
     */
    private final Properties overrideBrokerProperties = new Properties();

    /**
     * Definition of listeners defined on the broker.
     */
    private final List<BrokerListener> registeredListeners;

    /**
     * Properties defining how to connect to each available/registered listener on the broker.
     */
    private final List<ListenerProperties> listenerProperties = new ArrayList<>();

    /**
     * Default constructor, no overridden broker properties.
     */
    public KafkaTestServer() {
        this(new Properties());
    }

    /**
     * Alternative constructor allowing override of brokerProperties.
     * @param overrideBrokerProperties Define Kafka broker properties.
     * @throws IllegalArgumentException if overrideBrokerProperties argument is null.
     */
    public KafkaTestServer(final Properties overrideBrokerProperties) throws IllegalArgumentException {
        this(overrideBrokerProperties, new ArrayList<>());
    }

    /**
     * Alternative constructor allowing override of brokerProperties.
     * @param overrideBrokerProperties Define Kafka broker properties.
     * @param listeners Listener definitions.
     * @throws IllegalArgumentException if overrideBrokerProperties argument is null.
     */
    public KafkaTestServer(
        final Properties overrideBrokerProperties,
        final Collection<BrokerListener> listeners
    ) throws IllegalArgumentException {
        // Validate argument.
        if (overrideBrokerProperties == null) {
            throw new IllegalArgumentException("Cannot pass null overrideBrokerProperties argument.");
        }

        final List<BrokerListener> brokerListeners = new ArrayList<>();
        if (listeners == null || listeners.isEmpty()) {
            brokerListeners.add(new PlainListener());
        } else {
            brokerListeners.addAll(listeners);
        }

        // Add passed in properties.
        this.overrideBrokerProperties.putAll(overrideBrokerProperties);
        this.registeredListeners = Collections.unmodifiableList(new ArrayList<>(brokerListeners));
    }

    /**
     * Package protected constructor allowing override of ZookeeperTestServer instance.
     * @param overrideBrokerProperties Define Kafka broker properties.
     * @param zookeeperTestServer Zookeeper server instance to use.
     */
    KafkaTestServer(
        final Properties overrideBrokerProperties,
        final ZookeeperTestServer zookeeperTestServer,
        final Collection<BrokerListener> listeners
    ) {
        this(overrideBrokerProperties, listeners);

        // If instance is passed,
        if (zookeeperTestServer != null) {
            // We are no longer in charge of managing it.
            isManagingZookeeper = false;
        }
        // Save reference.
        this.zookeeperTestServer = zookeeperTestServer;
    }

    /**
     * bootstrap.servers string to configure Kafka consumers or producers to access the Kafka cluster.
     * @return Connect string to use for Kafka clients.
     */
    @Override
    public String getKafkaConnectString() {
        validateState(true, "Cannot get connect string prior to service being started.");

        // Return all of the connection properties.
        return listenerProperties.stream()
            .map(ListenerProperties::getConnectString)
            .collect(Collectors.joining(","));
    }

    @Override
    public List<ListenerProperties> getListenerProperties() {
        return Collections.unmodifiableList(listenerProperties);
    }

    /**
     * Returns an immutable list of broker hosts for the kafka cluster.
     * @return immutable list of hosts for brokers within the cluster.
     */
    @Override
    public KafkaBrokers getKafkaBrokers() {
        validateState(true, "Cannot get brokers before service has been started.");

        return new KafkaBrokers(
            Collections.singletonList(new KafkaBroker(this))
        );
    }

    /**
     * Returns the brokers Id.
     * @return This brokers Id.
     */
    public int getBrokerId() {
        validateState(true, "Cannot get brokerId prior to service being started.");
        return brokerConfig.brokerId();
    }

    /**
     * Returns properly formatted zookeeper connection string for zookeeper clients.
     * @return Connect string to use for Zookeeper clients.
     */
    public String getZookeeperConnectString() {
        validateState(true, "Cannot get connect string prior to service being started.");
        return zookeeperTestServer.getConnectString();
    }

    /**
     * Creates and starts ZooKeeper and Kafka server instances.
     * @throws Exception on startup errors.
     */
    public void start() throws Exception {
        // If we have no zkTestServer instance
        if (zookeeperTestServer == null) {
            // Create it.
            zookeeperTestServer = new ZookeeperTestServer();
        }

        // If we're managing the zookeeper instance
        if (isManagingZookeeper) {
            // Call restart which allows us to restart KafkaTestServer instance w/o issues.
            zookeeperTestServer.restart();
        } else {
            // If we aren't managing the Zookeeper instance, call start() to ensure it's been started.
            // Starting an already started instance is a NOOP.
            zookeeperTestServer.start();
        }

        // If broker has not yet been created...
        if (broker == null) {
            // Build properties using a baseline from overrideBrokerProperties.
            final Properties brokerProperties = new Properties();
            brokerProperties.putAll(overrideBrokerProperties);

            // Put required zookeeper connection properties.
            setPropertyIfNotSet(brokerProperties, "zookeeper.connect", zookeeperTestServer.getConnectString());

            // If log.dir is not set.
            if (brokerProperties.getProperty("log.dir") == null) {
                // Create temp path to store logs and set property.
                brokerProperties.setProperty("log.dir", Utils.createTempDirectory().getAbsolutePath());
            }

            // Ensure that we're advertising correct hostname appropriately
            setPropertyIfNotSet(brokerProperties, "host.name", getConfiguredHostname());

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

            // Loop over registered listeners and add each
            for (final BrokerListener listener : registeredListeners) {
                // Get port to listen on.
                final int port = listener.getNextPort();
                final String listenerDefinition = listener.getProtocol() + "://" + getConfiguredHostname() + ":" + port;
                listenerProperties.add(
                    new ListenerProperties(listener.getProtocol(), listenerDefinition, listener.getClientProperties())
                );
                appendProperty(brokerProperties, "advertised.listeners", listenerDefinition);
                appendProperty(brokerProperties,"listeners", listenerDefinition);

                // Apply other options
                brokerProperties.putAll(listener.getBrokerProperties());
            }

            // Retain the brokerConfig.
            brokerConfig = new KafkaConfig(brokerProperties);

            // Handle differences between Kafka versions.
            final Class<?> cl = Class.forName("kafka.server.KafkaServer");
            final Time time = Time.SYSTEM;
            final Option<String> threadNamePrefix = Option.apply("TestServer[" + brokerConfig.get("broker.id") + "]");
            try {
                // kafka_2.12 < 2.8.0
                final Constructor<?> constructor = cl.getConstructor(KafkaConfig.class, Time.class, Option.class, Seq.class);
                final Set<KafkaMetricsReporter> reporters = Collections.emptySet();
                broker = (KafkaServer) constructor.newInstance(brokerConfig, time, threadNamePrefix, JavaConverters.asScalaSet(reporters).toSeq());
            } catch (final NoSuchMethodException e) {
                // kafka_2.12 a>= 2.8.0
                final Constructor<?> constructor = cl.getConstructor(KafkaConfig.class, Time.class, Option.class, boolean.class);
                broker = (KafkaServer) constructor.newInstance(brokerConfig, time, threadNamePrefix, false);
            }
        }
        // Start broker.
        broker.startup();
    }

    /**
     * Closes the internal servers. Failing to call this at the end of your tests will likely
     * result in leaking instances.
     *
     * Provided alongside close() to stay consistent with start().
     * @throws Exception on shutdown errors.
     */
    public void stop() throws Exception {
        close();
    }

    /**
     * Closes the internal servers. Failing to call this at the end of your tests will likely
     * result in leaking instances.
     * @throws Exception on shutdown errors.
     */
    @Override
    public void close() throws Exception {
        if (broker != null) {
            // Shutdown and reset.
            broker.shutdown();
        }

        // Conditionally close zookeeper
        if (zookeeperTestServer != null && isManagingZookeeper) {
            // Call stop() on zk server instance.  This will not cleanup temp data.
            zookeeperTestServer.stop();
        }
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
     * If the key already exists on the properties, it will append the value to the existing value, comma delimited.
     * @param properties Properties to update.
     * @param key Key to update.
     * @param appendValue Value to set or append.
     * @return Updated value.
     */
    private Object appendProperty(final Properties properties, final String key, final String appendValue) {
        // Validate inputs
        if (properties == null) {
            throw new NullPointerException("properties argument cannot be null.");
        }
        if (key == null) {
            throw new NullPointerException("key argument cannot be null.");
        }

        String originalValue = properties.getProperty(key);
        if (originalValue != null && !originalValue.isEmpty()) {
            originalValue = originalValue + ", ";
        } else {
            originalValue = "";
        }
        properties.setProperty(key, originalValue + appendValue);

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
     * Helper method for ensure state consistency.
     * @param requireServiceStarted True if service should have been started, false if not.
     * @param errorMessage Error message to throw if the state is not consistent.
     * @throws IllegalStateException if the kafkaCluster state is not consistent.
     */
    private void validateState(final boolean requireServiceStarted, final String errorMessage) throws IllegalStateException {
        if (requireServiceStarted && broker == null) {
            throw new IllegalStateException(errorMessage);
        } else if (!requireServiceStarted && broker != null) {
            throw new IllegalStateException(errorMessage);
        }
    }
}