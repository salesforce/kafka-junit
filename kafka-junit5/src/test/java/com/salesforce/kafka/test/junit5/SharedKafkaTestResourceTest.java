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

import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.KafkaTestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Test of SharedKafkaTestResource.
 *
 * This also serves as an example of how to use this library!
 */
class SharedKafkaTestResourceTest {
    private static final Logger logger = LoggerFactory.getLogger(SharedKafkaTestResourceTest.class);

    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run.
     * It's automatically stopped after all of the tests are completed.
     *
     * This example we start a cluster with 2 brokers (defaults to a single broker) and configure the brokers to
     * disable topic auto-creation.
     *
     * It must be scoped as 'public static' in order for the appropriate startup/shutdown hooks to be called on the extension.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        // Start a cluster with 2 brokers.
        .withBrokers(2)
        // Disable topic auto-creation.
        .withBrokerProperty("auto.create.topics.enable", "false");


    /**
     * Validate that we started 2 brokers.
     */
    @Test
    void testTwoBrokersStarted() {
        final Collection<Node> nodes = getKafkaTestUtils().describeClusterNodes();
        Assertions.assertNotNull(nodes, "Sanity test, should not be null");
        Assertions.assertEquals(2, nodes.size(), "Should have two entries");

        // Grab id for each node found.
        final Set<Integer> foundBrokerIds = nodes.stream()
            .map(Node::id)
            .collect(Collectors.toSet());

        Assertions.assertEquals(2, foundBrokerIds.size(), "Found 2 brokers.");
        Assertions.assertTrue(foundBrokerIds.contains(1), "Found brokerId 1");
        Assertions.assertTrue(foundBrokerIds.contains(2), "Found brokerId 2");
    }

    /**
     * Example in a multi-broker cluster, how to stop an individual broker and bring it back on-line.
     */
    @Test
    void testBringingBrokerOffLine() throws Exception {
        // Now we want to test behavior by stopping brokerId 2.
        final KafkaBroker broker2 = sharedKafkaTestResource
            .getKafkaBrokers()
            .getBrokerById(2);

        // Shutdown broker Id 2.
        broker2.stop();

        // Describe the cluster
        List<Node> nodes = getKafkaTestUtils().describeClusterNodes();

        // We should only have 1 node now, and it should not include broker Id 2.
        Assertions.assertEquals(1, nodes.size());
        nodes.forEach((node) -> Assertions.assertNotEquals(2, node.id(), "Should not include brokerId 2"));

        // Test your applications behavior when a broker becomes unavailable or leadership changes.

        // Bring the broker back up
        broker2.start();

        // It may take a while for the broker to successfully rejoin the cluster,
        // Block until the broker has come on-line and then continue.
        getKafkaTestUtils()
            .waitForBrokerToComeOnLine(2, 10, TimeUnit.SECONDS);

        // We should have 2 nodes again.
        nodes = getKafkaTestUtils().describeClusterNodes();
        Assertions.assertEquals(2, nodes.size());

        // Collect the brokerIds in the cluster.
        final Set<Integer> foundBrokerIds = nodes.stream()
            .map(Node::id)
            .collect(Collectors.toSet());

        Assertions.assertTrue(foundBrokerIds.contains(1), "Found brokerId 1");
        Assertions.assertTrue(foundBrokerIds.contains(2), "Found brokerId 2");
    }

    /**
     * Test consuming and producing via KafkaProducer and KafkaConsumer instances.
     */
    @Test
    void testProducerAndConsumer() throws Exception {
        // Create a topic
        final String topicName = "ProducerAndConsumerTest" + System.currentTimeMillis();
        getKafkaTestUtils().createTopic(topicName, 1, (short) 1);

        final int partitionId = 0;

        // Define our message
        final String expectedKey = "my-key";
        final String expectedValue = "my test message";

        // Define the record we want to produce
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, partitionId, expectedKey, expectedValue);

        // Create a new producer
        try (final KafkaProducer<String, String> producer =
            getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class)) {

            // Produce it & wait for it to complete.
            final Future<RecordMetadata> future = producer.send(producerRecord);
            producer.flush();
            while (!future.isDone()) {
                Thread.sleep(500L);
            }
            logger.info("Produce completed");
        }

        // Create consumer
        try (final KafkaConsumer<String, String> kafkaConsumer =
            getKafkaTestUtils().getKafkaConsumer(StringDeserializer.class, StringDeserializer.class)) {

            final List<TopicPartition> topicPartitionList = new ArrayList<>();
            for (final PartitionInfo partitionInfo: kafkaConsumer.partitionsFor(topicName)) {
                topicPartitionList.add(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()));
            }
            kafkaConsumer.assign(topicPartitionList);
            kafkaConsumer.seekToBeginning(topicPartitionList);

            // Pull records from kafka, keep polling until we get nothing back
            ConsumerRecords<String, String> records;
            do {
                records = kafkaConsumer.poll(2000L);
                logger.info("Found {} records in kafka", records.count());
                for (ConsumerRecord<String, String> record: records) {
                    // Validate
                    Assertions.assertEquals(expectedKey, record.key(), "Key matches expected");
                    Assertions.assertEquals(expectedValue, record.value(), "value matches expected");
                }
            }
            while (!records.isEmpty());
        }
    }

    /**
     * Simple accessor.
     */
    private KafkaTestUtils getKafkaTestUtils() {
        return sharedKafkaTestResource.getKafkaTestUtils();
    }
}