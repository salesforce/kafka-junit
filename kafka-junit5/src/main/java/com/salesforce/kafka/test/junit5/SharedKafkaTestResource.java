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

package com.salesforce.kafka.test.junit5;

import com.salesforce.kafka.test.KafkaBrokerList;
import com.salesforce.kafka.test.KafkaCluster;
import com.salesforce.kafka.test.KafkaTestCluster;
import com.salesforce.kafka.test.KafkaTestUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Creates and stands up an internal test kafka server to be shared across test cases within the same test class.
 *
 * Example within your Test class.
 *
 *   &#064;RegisterExtension
 *   public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();
 *
 * Within your test case method:
 *   sharedKafkaTestResource.getKafkaTestServer()...
 */
public class SharedKafkaTestResource implements BeforeAllCallback, AfterAllCallback {
    private static final Logger logger = LoggerFactory.getLogger(SharedKafkaTestResource.class);

    /**
     * Our internal Kafka Test Server instance.
     */
    private KafkaCluster kafkaCluster = null;

    /**
     * Cached instance of KafkaTestUtils.
     */
    private KafkaTestUtils kafkaTestUtils = null;

    /**
     * Additional broker properties.
     */
    private final Properties brokerProperties = new Properties();

    /**
     * How many brokers to put into the cluster.
     */
    private int numberOfBrokers = 1;

    /**
     * Default constructor.
     */
    public SharedKafkaTestResource() {
        this(new Properties());
    }

    /**
     * Constructor allowing passing additional broker properties.
     * @param brokerProperties properties for Kafka broker.
     */
    public SharedKafkaTestResource(final Properties brokerProperties) {
        this.brokerProperties.putAll(brokerProperties);
    }

    /**
     * @return Instance of KafkaTestUtils configured and ready to go.
     */
    public KafkaTestUtils getKafkaTestUtils() {
        if (kafkaTestUtils == null) {
            kafkaTestUtils = new KafkaTestUtils(kafkaCluster);
        }
        return kafkaTestUtils;
    }

    /**
     * @return Connection string to connect to the Zookeeper instance.
     */
    public String getZookeeperConnectString() {
        return kafkaCluster.getZookeeperConnectString();
    }

    /**
     * @return The proper connect string to use for Kafka.
     */
    public String getKafkaConnectString() {
        return kafkaCluster.getKafkaConnectString();
    }

    /**
     * @return Immutable list of brokers, indexed by their brokerIds.
     */
    public KafkaBrokerList getKafkaBrokers() {
        return kafkaCluster.getKafkaBrokers();
    }

    /**
     * Helper to allow overriding Kafka broker properties.  Can only be called prior to the service
     * being started.
     * @param name Kafka broker configuration property name.
     * @param value Value to set for the configuration property.
     * @return SharedKafkaTestResource instance for method chaining.
     * @throws IllegalArgumentException if name argument is null.
     * @throws IllegalStateException if method called after service has started.
     */
    public SharedKafkaTestResource withBrokerProperty(final String name, final String value) {
        // Validate input.
        if (name == null) {
            throw new IllegalArgumentException("Cannot pass null name argument");
        }

        // Validate state.
        if (kafkaCluster != null) {
            throw new IllegalStateException("Cannot add properties after service has started");
        }

        // Add or set property.
        if (value == null) {
            brokerProperties.remove(name);
        } else {
            brokerProperties.put(name, value);
        }
        return this;
    }

    /**
     * Set how many brokers to include in the test cluster.
     * @param brokerCount The number of brokers.
     * @return SharedKafkaTestResource for method chaining.
     */
    public SharedKafkaTestResource withBrokers(final int brokerCount) {
        // Validate state.
        if (kafkaCluster != null) {
            throw new IllegalStateException("Cannot set brokers after service has started");
        }

        if (brokerCount < 1) {
            throw new IllegalArgumentException("Cannot have 0 or fewer brokers");
        }
        this.numberOfBrokers = brokerCount;
        return this;
    }


    /**
     * Here we stand up an internal test kafka and zookeeper service.
     * Once for all tests that use this shared resource.
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        logger.info("Starting kafka test server");
        if (kafkaCluster != null) {
            throw new IllegalStateException("Unknown State!  Kafka Test Server already exists!");
        }
        // Setup kafka test server
        kafkaCluster = new KafkaTestCluster(numberOfBrokers, brokerProperties);
        kafkaCluster.start();
    }

    /**
     * Here we shut down the internal test kafka and zookeeper services.
     */
    @Override
    public void afterAll(ExtensionContext context) {
        logger.info("Shutting down kafka test server");

        // Close out kafka test server if needed
        if (kafkaCluster == null) {
            return;
        }
        try {
            kafkaCluster.close();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        kafkaCluster = null;
    }
}
