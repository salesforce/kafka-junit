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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class KafkaTestClusterTest {
    /**
     * This test validates that the cluster contains 2 nodes
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
        }
    }

    /**
     * This test showing/validating creating a topic across multiple brokers.
     */
    @Test
    void testCreateTopicAcrossMultipleBrokers() throws Exception {
        final int numberOfBrokers = 2;
        final String topicName = "MultiBrokerTest2-" + System.currentTimeMillis();

        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
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
     * This test more serves as an example of how to interact with the test Kafka brokers.
     *
     * This test does the following:
     *      - Starts a 2 node cluster.
     *      - Creates a topic with Partition Count = 2, ReplicationFactor = 2.
     *      - Publishes 2 messages to each partition (4 messages total)
     *      - Stops brokerId 2.
     *      - Consumes from topic from remaining broker.
     *      - Validates that all messages are retrieved, including those that were originally published
     *        to the broker which is now off-line.
     */
    @Test
    void testConsumingFromMultiBrokerClusterWhenBrokerIsStopped() throws Exception {
        final int numberOfBrokers = 2;
        final int numberOfPartitions = 2;
        final int numberOfMessagesPerPartition = 2;
        final String topicName = "MultiBrokerTest3-" + System.currentTimeMillis();

        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Start the cluster
            kafkaTestCluster.start();

            // Create test utils instance.
            final KafkaTestUtils testUtils = new KafkaTestUtils(kafkaTestCluster);

            // Create the topic.
            testUtils.createTopic(topicName, numberOfPartitions, (short) numberOfBrokers);

            // Describe the topic.
            final TopicDescription topicDescription = testUtils.describeTopic(topicName);

            // Validate it has 2 partitions
            Assertions.assertEquals(numberOfPartitions, topicDescription.partitions().size(), "Should have multiple partitions");

            // Validate more than one broker owns the partitions.
            final Set<Integer> leaderIds = new HashSet<>();
            topicDescription.partitions().forEach((topicPartitionInfo) -> leaderIds.add(topicPartitionInfo.leader().id()));
            Assertions.assertEquals(numberOfBrokers, leaderIds.size(), "Should have multiple leaders");

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
            kafkaTestCluster.getKafkaBrokerById(2).close();

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
}