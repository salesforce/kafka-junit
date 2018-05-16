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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.salesforce.kafka.test.KafkaTestServer;
import org.apache.kafka.clients.admin.AdminClient;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     * This example we override the Kafka broker id to '12', but this serves as an example of how you
     * how you could override any Kafka broker property.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        .withBrokerProperty("broker.id", "1000");

    /**
     * Before every test, we generate a random topic name and create it within the embedded kafka server.
     * Each test can then be segmented run any tests against its own topic.
     */
    private String topicName;

    /**
     * This happens once before every test method.
     * Create a new empty namespace with randomly generated name.
     */
    @BeforeEach
    void beforeTest() {
        // Generate topic name
        topicName = getClass().getSimpleName() + Clock.systemUTC().millis();

        // Create topic with a single partition,
        // NOTE: This will create partition id 0, because partitions are indexed at 0 :)
        getKafkaTestServer().createTopic(topicName, 1);
    }

    /**
     * Test that KafkaServer works as expected!
     *
     * This also serves as a decent example of how to use the producer and consumer.
     */
    @Test
    void testProducerAndConsumer() throws Exception {
        final int partitionId = 0;

        // Define our message
        final String expectedKey = "my-key";
        final String expectedValue = "my test message";

        // Define the record we want to produce
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, partitionId, expectedKey, expectedValue);

        // Create a new producer
        KafkaProducer<String, String> producer = getKafkaTestServer().getKafkaProducer(StringSerializer.class, StringSerializer.class);

        // Produce it & wait for it to complete.
        Future<RecordMetadata> future = producer.send(producerRecord);
        producer.flush();
        while (!future.isDone()) {
            Thread.sleep(500L);
        }
        logger.info("Produce completed");

        // Close producer!
        producer.close();

        KafkaConsumer<String, String> kafkaConsumer =
            getKafkaTestServer().getKafkaConsumer(StringDeserializer.class, StringDeserializer.class);

        final List<TopicPartition> topicPartitionList = Lists.newArrayList();
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
                assertEquals(expectedKey, record.key(), "Key matches expected");
                assertEquals(expectedValue, record.value(), "value matches expected");
            }
        }
        while (!records.isEmpty());

        // close consumer
        kafkaConsumer.close();
    }

    /**
     * Test if we create a topic more than once, no errors occur.
     */
    @Test
    void testCreatingTopicMultipleTimes() {
        final String myTopic = "myTopic";
        for (int creationCounter = 0; creationCounter < 5; creationCounter++) {
            getKafkaTestServer().createTopic(myTopic);
        }
        assertTrue(true, "Made it here!");
    }

    /**
     * Validate that broker Id was overridden correctly.
     */
    @Test
    void testBrokerIdOverride() throws ExecutionException, InterruptedException {
        try (final AdminClient adminClient = getKafkaTestServer().getAdminClient()) {
            final Collection<Node> nodes = adminClient.describeCluster().nodes().get();

            assertNotNull(nodes, "Sanity test, should not be null");
            assertEquals(1, nodes.size(), "Should have 1 entry");

            // Get details about our test broker/node
            final Node node = Iterables.get(nodes, 0);

            // Validate
            assertEquals(1000, node.id(), "Has expected overridden broker Id");
        }
    }

    /**
     * Simple accessor.
     */
    private KafkaTestServer getKafkaTestServer() {
        return sharedKafkaTestResource.getKafkaTestServer();
    }
}
