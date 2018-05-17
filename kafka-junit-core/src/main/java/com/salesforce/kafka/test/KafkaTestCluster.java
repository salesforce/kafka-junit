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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Utility for setting up a Cluster of KafkaTestServers.
 */
public class KafkaTestCluster implements AutoCloseable {

    /**
     * Internal Test Zookeeper service shared by all Kafka brokers.
     */
    private final ZookeeperTestServer zkTestServer = new ZookeeperTestServer();

    /**
     * Defines how many brokers will be created and started within the cluster.
     */
    private final int numberOfBrokers;

    /**
     * List containing all of the brokers in the cluster.  Since each broker is quickly accessible via it's 'brokerId' property
     * by simply removing 1 from it's id and using it as the index into the list.
     *
     * Example: to get brokerId = 4, retrieve index 3 in this list.
     */
    private final List<KafkaTestServer> brokers = new ArrayList<>();

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

    /**
     * Starts the cluster.
     * @throws Exception on startup errors.
     */
    public void start() throws Exception {
        // If our brokers list is not empty
        if (!brokers.isEmpty()) {
            // That means we've already started the cluster.
            throw new IllegalStateException("Cluster has already been started.");
        }

        // Start Zookeeper instance.
        zkTestServer.start();

        // Loop over brokers
        for (int brokerId = 1; brokerId <= numberOfBrokers; brokerId++) {
            // Create properties for brokers
            final Properties brokerProperties = new Properties();
            brokerProperties.put("broker.id", String.valueOf(brokerId));

            // Create new KafkaTestServer
            final KafkaTestServer kafkaBroker = new KafkaTestServer(brokerProperties, zkTestServer.getZookeeperTestServer());

            // Start it
            kafkaBroker.start();

            // Add to our broker list
            brokers.add(kafkaBroker);
        }
    }

    /**
     * @return immutable list of brokers within the cluster.
     */
    public List<KafkaTestServer> getKafkaBrokers() {
        return Collections.unmodifiableList(brokers);
    }

    /**
     * Retrieve a broker by its brokerId.
     * @param brokerId the Id of the broker to retrieve.
     * @return KafkaTestServer instance for the given broker Id.
     */
    public KafkaTestServer getKafkaBrokerById(final int brokerId) {
        // Brokers are zero indexed in the array.
        return brokers.get(brokerId - 1);
    }

    /**
     * Shuts the cluster down.
     * @throws Exception on shutdown errors.
     */
    @Override
    public void close() throws Exception {
        // Loop over brokers
        for (final KafkaTestServer kafkaBroker : brokers) {
            kafkaBroker.close();
        }

        // Empty our list of brokers.
        brokers.clear();

        // Stop zkServer
        zkTestServer.stop();
    }
}
