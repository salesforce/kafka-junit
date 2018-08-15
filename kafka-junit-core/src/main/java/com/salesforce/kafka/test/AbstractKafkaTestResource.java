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

import java.util.Properties;

/**
 * Shared code between JUnit4 and JUnit5 shared resources.
 * @param <T> The concrete implementation of this class, to allow for method chaining.
 */
public abstract class AbstractKafkaTestResource<T extends AbstractKafkaTestResource<T>> {
    /**
     * Our internal Kafka Test Server instance.
     */
    private KafkaCluster kafkaCluster = null;

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
    public AbstractKafkaTestResource() {
        this(new Properties());
    }

    /**
     * Constructor allowing passing additional broker properties.
     * @param brokerProperties properties for Kafka broker.
     */
    public AbstractKafkaTestResource(final Properties brokerProperties) {
        this.brokerProperties.putAll(brokerProperties);
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
    @SuppressWarnings("unchecked")
    public T withBrokerProperty(final String name, final String value) {
        // Validate state.
        validateState(false, "Cannot add properties after service has started.");

        // Validate input.
        if (name == null) {
            throw new IllegalArgumentException("Cannot pass null name argument");
        }

        // Add or set property.
        if (value == null) {
            brokerProperties.remove(name);
        } else {
            brokerProperties.put(name, value);
        }
        return (T) this;
    }

    /**
     * Set how many brokers to include in the test cluster.
     * @param brokerCount The number of brokers.
     * @return SharedKafkaTestResource for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T withBrokers(final int brokerCount) {
        // Validate state.
        validateState(false, "Cannot set brokers after service has started.");

        if (brokerCount < 1) {
            throw new IllegalArgumentException("Cannot have 0 or fewer brokers");
        }
        this.numberOfBrokers = brokerCount;
        return (T) this;
    }

    /**
     * KafkaTestUtils is a collection of re-usable/common access patterns for interacting with the Kafka cluster.
     * @return Instance of KafkaTestUtils configured to operate on the Kafka cluster.
     */
    public KafkaTestUtils getKafkaTestUtils() {
        // Validate internal state.
        validateState(true, "Cannot access KafkaTestUtils before Kafka service has been started.");
        return new KafkaTestUtils(kafkaCluster);
    }

    /**
     * Returns connection string for zookeeper clients.
     * @return Connection string to connect to the Zookeeper instance.
     */
    public String getZookeeperConnectString() {
        validateState(true, "Cannot access Zookeeper before service has been started.");
        return kafkaCluster.getZookeeperConnectString();
    }

    /**
     * bootstrap.servers string to configure Kafka consumers or producers to access the Kafka cluster.
     * @return Connect string to use for Kafka clients.
     */
    public String getKafkaConnectString() {
        validateState(true, "Cannot access Kafka before service has been started.");
        return kafkaCluster.getKafkaConnectString();
    }

    /**
     * Returns an immutable list of broker hosts for the kafka cluster.
     * @return immutable list of hosts for brokers within the cluster.
     */
    public KafkaBrokers getKafkaBrokers() {
        validateState(true, "Cannot access Kafka before service has been started.");
        return kafkaCluster.getKafkaBrokers();
    }

    protected KafkaCluster getKafkaCluster() {
        return kafkaCluster;
    }

    protected void setKafkaCluster(final KafkaCluster kafkaCluster) {
        this.kafkaCluster = kafkaCluster;
    }

    protected Properties getBrokerProperties() {
        return brokerProperties;
    }

    protected int getNumberOfBrokers() {
        return numberOfBrokers;
    }

    /**
     * Helper method for ensure state consistency.
     * @param shouldKafkaExistYet True if KafkaCluster should exist, false if it should not.
     * @param errorMessage Error message to throw if the state is not consistent.
     * @throws IllegalStateException if the kafkaCluster state is not consistent.
     */
    protected void validateState(final boolean shouldKafkaExistYet, final String errorMessage) throws IllegalStateException {
        if (shouldKafkaExistYet && kafkaCluster == null) {
            throw new IllegalStateException(errorMessage);
        } else if (!shouldKafkaExistYet && kafkaCluster != null) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
