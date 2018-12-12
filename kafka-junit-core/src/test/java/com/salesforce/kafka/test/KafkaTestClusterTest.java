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

import com.salesforce.kafka.test.listeners.BrokerListener;
import com.salesforce.kafka.test.listeners.PlainListener;
import com.salesforce.kafka.test.listeners.SaslPlainListener;
import com.salesforce.kafka.test.listeners.SaslSslListener;
import com.salesforce.kafka.test.listeners.SslListener;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

class KafkaTestClusterTest {

    /**
     * This test attempts to start a cluster with 2 brokers. It then validates that when the cluster
     * is started, the correct number of brokers were brought on-line.
     */
    @Test
    void testMultipleNodesInBroker() throws Exception {
        final int numberOfBrokers = 2;

        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Start the cluster
            kafkaTestCluster.start();

            final Set<Integer> foundBrokerIds = new HashSet<>();
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestCluster);
            final Collection<Node> nodes = kafkaTestUtils.describeClusterNodes();

            Assertions.assertNotNull(nodes, "Sanity test, should not be null");
            Assertions.assertEquals(2, nodes.size(), "Should have two entries");

            // Grab id for each node found.
            nodes.forEach(
                (node) -> foundBrokerIds.add(node.id())
            );

            Assertions.assertEquals(2, foundBrokerIds.size(), "Found 2 brokers.");
            Assertions.assertTrue(foundBrokerIds.contains(1), "Found brokerId 1");
            Assertions.assertTrue(foundBrokerIds.contains(2), "Found brokerId 2");

            // Call getKafkaBrokers()
            final KafkaBrokers brokers = kafkaTestCluster.getKafkaBrokers();

            // Validate
            Assertions.assertNotNull(brokers, "Should have non-null result.");
            Assertions.assertEquals(numberOfBrokers, brokers.size(), "Should have 3 brokers.");

            validateKafkaBroker(brokers.getBrokerById(1), 1);
            validateKafkaBroker(brokers.getBrokerById(2), 2);

            // Now ask for an invalid broker.
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                brokers.getBrokerById(0);
            });

            // Now ask for an invalid broker.
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                brokers.getBrokerById(3);
            });

            // Validate getKafkaBrokerById
            validateKafkaBroker(kafkaTestCluster.getKafkaBrokerById(1), 1);
            validateKafkaBroker(kafkaTestCluster.getKafkaBrokerById(2), 2);

            // Now ask for an invalid broker.
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                kafkaTestCluster.getKafkaBrokerById(0);
            });
        }
    }

    /**
     * This test calls getKafkaBrokers() before the cluster has started. It is expected to throw an IllegalStateException
     * in this scenario.
     */
    @Test
    void testGetKafkaBrokersBeforeClusterHasStarted() throws Exception {
        final int numberOfBrokers = 2;

        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Call getKafkaBrokers() before the cluster has started.
            Assertions.assertThrows(IllegalStateException.class, () -> {
                kafkaTestCluster.getKafkaBrokers();
            });
        }
    }

    /**
     * This test calls getKafkaBrokerId() before the cluster has been properly started. It is expected to throw an IllegalStateException
     * in this scenario.
     */
    @Test
    void testGetKafkaBrokerByIdBeforeClusterStarted() throws Exception {
        final int numberOfBrokers = 2;

        // Create cluster
        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Call getKafkaBrokerById() before the cluster is started, it should throw exceptions.
            Assertions.assertThrows(IllegalStateException.class, () -> kafkaTestCluster.getKafkaBrokerById(0));
            Assertions.assertThrows(IllegalStateException.class, () -> kafkaTestCluster.getKafkaBrokerById(1));
            Assertions.assertThrows(IllegalStateException.class, () -> kafkaTestCluster.getKafkaBrokerById(2));
        }
    }

    /**
     * This test calls getKafkaConnectString() before the cluster has been properly started.
     * It is expected to throw an IllegalStateException in this scenario.
     */
    @Test
    void testGetKafkaConnectStringBeforeClusterIsStarted() throws Exception {
        final int numberOfBrokers = 2;

        // Create cluster
        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Call getKafkaBrokerById() before the cluster is started, it should throw exceptions.
            Assertions.assertThrows(IllegalStateException.class, () -> kafkaTestCluster.getKafkaConnectString());
        }
    }

    /**
     * This test calls getKafkaBrokers() after the cluster has been properly started. It is expected
     * to return proper connect strings for each of the brokers.
     */
    @Test
    void testGetKafkaConnectString() throws Exception {
        final int numberOfBrokers = 3;

        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Start cluster
            kafkaTestCluster.start();

            // Create test Utils
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestCluster);

            // Ask for the connect string
            final String resultStr = kafkaTestCluster.getKafkaConnectString();
            Assertions.assertNotNull(resultStr, "Should have non-null result");

            // Split the result by commas to get individual hosts.
            final Set<String> hosts = new HashSet<>(Arrays.asList(resultStr.split(",")));
            Assertions.assertEquals(numberOfBrokers, hosts.size(), "Should contain 3 entries.");

            // Ask for which nodes exist in the cluster
            final List<Node> nodes = kafkaTestUtils.describeClusterNodes();

            // Sanity test
            Assertions.assertEquals(numberOfBrokers, nodes.size(), "Should have 3 brokers in the cluster");

            // Make sure each node is represented properly.
            for (final Node node: nodes) {
                final String calculatedConnectString = "PLAINTEXT://" + node.host() + ":" + node.port();
                Assertions.assertTrue(hosts.contains(calculatedConnectString), "Should contain " + calculatedConnectString);
            }
        }
    }

    /**
     * This test starts a cluster with 2 brokers. It then attempts to create a topic
     * that spans both brokers.  It acts mostly as a sanity test vs validating behavior of the library.
     */
    @Test
    void testCreateTopicAcrossMultipleBrokers() throws Exception {
        final int numberOfBrokers = 2;
        final String topicName = "MultiBrokerTest2-" + System.currentTimeMillis();

        try (final KafkaTestCluster kafkaTestCluster
            = new KafkaTestCluster(numberOfBrokers, getDefaultBrokerOverrideProperties())) {

            // Start the cluster
            kafkaTestCluster.start();

            // Create test utils instance.
            final KafkaTestUtils testUtils = new KafkaTestUtils(kafkaTestCluster);

            // Define a new topic with 2 partitions, with replication factor of 2.
            testUtils.createTopic(topicName, numberOfBrokers, (short) numberOfBrokers);

            // Lets describe the topic.
            final TopicDescription topicDescription = testUtils.describeTopic(topicName);

            // Validate has 2 partitions
            Assertions.assertEquals(numberOfBrokers, topicDescription.partitions().size(), "Correct number of partitions.");

            // Validate the partitions have 2 replicas
            for (final TopicPartitionInfo topicPartitionInfo : topicDescription.partitions()) {
                Assertions.assertEquals(numberOfBrokers, topicPartitionInfo.replicas().size(), "Should have 2 replicas");
                Assertions.assertEquals(numberOfBrokers, topicPartitionInfo.isr().size(), "Should have 2 In-Sync-Replicas");
            }
        }
    }

    /**
     * Sanity test that a 2 node cluster behaves how we would expect it to.  It also serves as an example
     * of how you can start a multi-node cluster and then individually shutdown a broker to validate
     * the behavior of your application.
     *
     * This test does the following:
     *      - Starts a 2 node cluster.
     *      - Creates a topic with Partition Count = 2, ReplicationFactor = 2.
     *      - Publishes 2 messages to each partition (4 messages total)
     *      - Stops brokerId 2.  At this point the partition broker2 was the leader for should be transferred to broker1.
     *      - Consumes from topic from remaining broker.
     *      - Validates that all messages are retrieved, including those that were originally published
     *        to the broker which is now off-line.
     */
    @Test
    void testConsumingFromMultiBrokerClusterWhenBrokerIsStopped() throws Exception {
        final int numberOfBrokers = 2;
        final int numberOfPartitions = 2;
        final int numberOfMessagesPerPartition = 2;
        final int replicaFactor = 2;
        final String topicName = "MultiBrokerTest3-" + System.currentTimeMillis();

        try (final KafkaTestCluster kafkaTestCluster
            = new KafkaTestCluster(numberOfBrokers)) {

            // Start the cluster
            kafkaTestCluster.start();

            // Create test utils instance.
            final KafkaTestUtils testUtils = new KafkaTestUtils(kafkaTestCluster);

            // Create the topic, 2 partitions, replica factor of 2
            testUtils.createTopic(topicName, numberOfPartitions, (short) replicaFactor);

            // Describe the topic.
            final TopicDescription topicDescription = testUtils.describeTopic(topicName);

            // Validate it has 2 partitions
            Assertions.assertEquals(numberOfPartitions, topicDescription.partitions().size(), "Should have multiple partitions");

            // Validate each partition belongs to a different broker, and each partition has 1 ISRs.
            final Set<Integer> leaderIds = new HashSet<>();
            for (final TopicPartitionInfo partitionInfo : topicDescription.partitions()) {
                // Each partition should have 2 ISRs
                Assertions.assertEquals(
                    replicaFactor,
                    partitionInfo.isr().size(),
                    "Partition " + partitionInfo.partition() + " missing ISR"
                );

                // Add leader Id to set.
                leaderIds.add(partitionInfo.leader().id());
            }
            Assertions.assertEquals(2, leaderIds.size(), "Should have two leaders");

            // Attempt to publish into each partition in the topic.
            for (int partitionId = 0; partitionId < numberOfPartitions; partitionId++) {
                // Produce records.
                final List<ProducedKafkaRecord<byte[], byte[]>> producedRecords
                    = testUtils.produceRecords(numberOfMessagesPerPartition, topicName, partitionId);

                // Lets do some simple validation
                for (final ProducedKafkaRecord producedRecord: producedRecords) {
                    Assertions.assertEquals(partitionId, producedRecord.getPartition(), "Should be on correct partition");
                    Assertions.assertEquals(topicName, producedRecord.getTopic(), "Should be on correct topic");
                }
            }

            // Stop brokerId 2.
            kafkaTestCluster
                .getKafkaBrokerById(2)
                .stop();

            // It may take a moment for the broker to cleanly shut down.
            List<Node> nodes;
            for (int attempts = 0; attempts <= 5; attempts++) {
                // Describe the cluster and wait for it to go to 1 broker.
                nodes = testUtils.describeClusterNodes();
                if (nodes.size() == 1) {
                    break;
                }
                Thread.sleep(1000L);
            }

            // Consume all messages
            final List<ConsumerRecord<byte[], byte[]>> consumedRecords = testUtils.consumeAllRecordsFromTopic(topicName);

            // Validate we have (numberOfMessagesPerPartition * numberOfPartitions) records.
            Assertions.assertEquals(
                (numberOfMessagesPerPartition * numberOfPartitions),
                consumedRecords.size(),
                "Found all records in kafka."
            );
        }
    }

    /**
     * This test attempts to start a cluster with 2 brokers, produce some data into the cluster, and then shut it down.
     * It then starts the cluster instance back up and attempts to consume the original messages.
     */
    @Test
    void testRestartingCluster() throws Exception {
        final int numberOfBrokers = 2;
        final String topicName = "RestartClusterTest-" + System.currentTimeMillis();

        try (final KafkaTestCluster kafkaTestCluster
            = new KafkaTestCluster(numberOfBrokers, getDefaultBrokerOverrideProperties())) {

            // Start the cluster
            kafkaTestCluster.start();

            // Create kafka test utils
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestCluster);

            // Create a topic with 2 partitions, replica factor of 1 to avoid unclean shutdown
            kafkaTestUtils.createTopic(topicName, numberOfBrokers, (short) 1);

            // Produce data into each partition of the topic
            for (int partitionId = 0; partitionId < numberOfBrokers; partitionId++) {
                kafkaTestUtils.produceRecords(2, topicName, partitionId);
            }

            // Shutdown cluster.
            kafkaTestCluster.stop();

            // Start it back up
            kafkaTestCluster.start();

            // Attempt to consume records back out of the cluster after being restarted.
            final List<ConsumerRecord<byte[], byte[]>> records = kafkaTestUtils.consumeAllRecordsFromTopic(topicName);

            // Validate
            Assertions.assertEquals((numberOfBrokers * 2), records.size());
        }
    }

    /**
     * Helper method to validate a KafkaBroker instance.
     * @param broker The KafkaBroker instance under test.
     * @param expectedBrokerId The expected brokerId.
     */
    private void validateKafkaBroker(final KafkaBroker broker, final int expectedBrokerId) {
        Assertions.assertNotNull(broker);
        Assertions.assertEquals(expectedBrokerId, broker.getBrokerId());
    }

    /**
     * Test a multi-node cluster instance with various listeners.
     * @param listeners The listeners to register.
     */
    @ParameterizedTest
    @MethodSource("provideListeners")
    void testCustomizedListeners(final List<BrokerListener> listeners) throws Exception {
        final String topicName = "testRestartingBroker-" + System.currentTimeMillis();
        final int expectedMsgCount = 2;
        final int numberOfBrokers = 2;

        // Speed up shutdown in our tests
        final Properties overrideProperties = getDefaultBrokerOverrideProperties();

        // Create our test server instance
        try (final KafkaTestCluster kafkaTestCluster =
            new KafkaTestCluster(numberOfBrokers, overrideProperties, listeners)) {
            // Start broker
            kafkaTestCluster.start();

            // Create KafkaTestUtils
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestCluster);

            // Create topic
            kafkaTestUtils.createTopic(topicName, 1, (short) numberOfBrokers);

            // Publish 2 messages into topic
            kafkaTestUtils.produceRecords(expectedMsgCount, topicName, 0);

            // Sanity test - Consume the messages back out before shutting down broker.
            final List<ConsumerRecord<byte[], byte[]>> records = kafkaTestUtils.consumeAllRecordsFromTopic(topicName);
            Assertions.assertNotNull(records);
            Assertions.assertEquals(expectedMsgCount, records.size(), "Should have found 2 records.");

            // Call stop/close on the broker
            kafkaTestCluster.stop();
        }
    }

    /**
     * Define various listeners.
     */
    private static Stream<Arguments> provideListeners() {
        // Create default plain listener
        final BrokerListener plainListener = new PlainListener();

        // Create SSL listener
        final BrokerListener sslListener = new SslListener()
            .withClientAuthRequired()
            .withKeyStoreLocation(KafkaTestServer.class.getClassLoader().getResource("kafka.keystore.jks").getFile())
            .withKeyStorePassword("password")
            .withTrustStoreLocation(KafkaTestServer.class.getClassLoader().getResource("kafka.truststore.jks").getFile())
            .withTrustStorePassword("password")
            .withKeyPassword("password");

        // Create SASL_PLAIN listener.
        final BrokerListener saslPlainListener = new SaslPlainListener()
            .withUsername("kafkaclient")
            .withPassword("client-secret");

        // Create SASL_SSL listener
        final BrokerListener saslSslListener = new SaslSslListener()
            .withClientAuthRequired()
            .withUsername("kafkaclient")
            .withPassword("client-secret")
            .withKeyStoreLocation(KafkaTestServer.class.getClassLoader().getResource("kafka.keystore.jks").getFile())
            .withKeyStorePassword("password")
            .withTrustStoreLocation(KafkaTestServer.class.getClassLoader().getResource("kafka.truststore.jks").getFile())
            .withTrustStorePassword("password")
            .withKeyPassword("password");

        final List<BrokerListener> listenersGroup1 = new ArrayList<>();
        listenersGroup1.add(plainListener);
        listenersGroup1.add(sslListener);

        final List<BrokerListener> listenersGroup2 = new ArrayList<>();
        listenersGroup2.add(sslListener);
        listenersGroup2.add(saslPlainListener);

        return Stream.of(
            // Just plain
            Arguments.of(Collections.singletonList(plainListener)),

            // Just SSL
            Arguments.of(Collections.singletonList(sslListener)),

            // Just SASL_PLAIN
            Arguments.of(Collections.singletonList(saslPlainListener)),

            // Just SASL_SSL
            Arguments.of(Collections.singletonList(saslSslListener)),

            // Combination of plain and SSL
            Arguments.of(listenersGroup1),

            // Combination of SSL and SaslPlain
            Arguments.of(listenersGroup2)
        );
    }

    private Properties getDefaultBrokerOverrideProperties() {
        // Speed up shutdown in our tests
        final Properties overrideProperties = new Properties();
        overrideProperties.setProperty("controlled.shutdown.max.retries", "0");
        overrideProperties.setProperty("controlled.shutdown.enable", "true");
        overrideProperties.setProperty("controlled.shutdown.retry.backoff.ms", "100");
        return overrideProperties;
    }
}