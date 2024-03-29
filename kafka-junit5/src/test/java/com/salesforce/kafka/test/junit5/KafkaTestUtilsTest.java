/**
 * Copyright (c) 2017-2021, Salesforce.com, Inc.
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

import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.ProducedKafkaRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Serves as an example of how to make use of KafkaTestUtils.
 */
class KafkaTestUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTestUtilsTest.class);

    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     *
     * It must be scoped as 'public static' in order for the appropriate startup/shutdown hooks to be called on the extension.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();

    /**
     * Before every test, we generate a random topic name and create it within the embedded kafka server.
     * Each test can then be segmented run any tests against its own topic.
     */
    private String topicName;

    /**
     * This happens once before every test method.
     * Create a new topic with unique name.
     */
    @BeforeEach
    void beforeTest() {
        // Generate unique topic name
        topicName = getClass().getSimpleName() + Clock.systemUTC().millis();

        // Create topic with 3 partitions,
        // NOTE: This will create partition ids 0 through 2, because partitions are indexed at 0 :)
        getKafkaTestUtils().createTopic(topicName, 3, (short) 1);
    }

    /**
     * Example showing how to get information about a topic in your Kafka cluster.
     */
    @Test
    void testDescribeTopic() {
        final TopicDescription topicDescription = getKafkaTestUtils().describeTopic(topicName);
        Assertions.assertNotNull(topicDescription, "Should return a result");

        // Debug logging.
        logger.info("Found topic with name {} and {} partitions.", topicDescription.name(), topicDescription.partitions().size());

        Assertions.assertEquals(3, topicDescription.partitions().size(), "Topic should have 3 partitions");
        Assertions.assertFalse(topicDescription.isInternal(), "Our topic is not an internal topic");
        Assertions.assertEquals(topicName, topicDescription.name(), "Has the correct name");
    }

    /**
     * Example showing how to get information about brokers in your Kafka cluster.
     */
    @Test
    void testDescribeClusterNodes() {
        final List<Node> clusterNodes = getKafkaTestUtils().describeClusterNodes();
        Assertions.assertNotNull(clusterNodes, "Should return a result");

        // Debug logging
        logger.info("Found {} nodes in your cluster", clusterNodes.size());

        Assertions.assertFalse(clusterNodes.isEmpty(), "Should not be empty");
        Assertions.assertEquals(1, clusterNodes.size(), "Should have one node");
        Assertions.assertEquals(1, clusterNodes.get(0).id(), "Should have brokerId 1");
    }

    /**
     * Example showing how to get an AdminClient connected to your Kafka cluster.
     */
    @Test
    void testGetAdminClient() {
        // Get admin client connected to Kafka cluster.
        try (final AdminClient adminClient = getKafkaTestUtils().getAdminClient()) {
            // Use to do whatever you need.
            Assertions.assertNotNull(adminClient);
        }
    }

    /**
     * Simple example of how to produce records into a topic.
     */
    @Test
    void testProducerAndConsumer() {
        final int numberOfRecords = 12;

        // Create our utility class
        final KafkaTestUtils kafkaTestUtils = getKafkaTestUtils();

        // Get a producer
        try (final KafkaProducer<String, String> producer
            = kafkaTestUtils.getKafkaProducer(StringSerializer.class, StringSerializer.class)) {

            // Produce 12 records
            for (int recordCount = 0; recordCount < numberOfRecords; recordCount++) {
                // Create a record.
                final ProducerRecord<String, String> record = new ProducerRecord<>(
                    topicName,
                    "My Key " + recordCount,
                    "My Value " + recordCount
                );
                producer.send(record);
            }
            // Ensure messages are flushed.
            producer.flush();
        }

        // Consume records back out
        final List<ConsumerRecord<String, String>> consumerRecords
            = kafkaTestUtils.consumeAllRecordsFromTopic(topicName, StringDeserializer.class, StringDeserializer.class);

        Assertions.assertNotNull(consumerRecords, "Should have non-null result.");
        Assertions.assertEquals(numberOfRecords, consumerRecords.size(), "Should have 10 records.");

        // Log the records we found.
        for (final ConsumerRecord<String, String> consumerRecord : consumerRecords) {
            logger.info(
                "Found Key: {} on Partition: {} with Value: {}",
                consumerRecord.key(),
                consumerRecord.partition(),
                consumerRecord.value()
            );
        }
    }

    /**
     * Sometimes you don't care what the contents of the records you produce or consume are.
     */
    @Test
    void testProducerAndConsumerUtils() {
        final int numberOfRecords = 10;
        final int partitionId = 2;

        // Create our utility class
        final KafkaTestUtils kafkaTestUtils = getKafkaTestUtils();

        // Produce some random records
        final List<ProducedKafkaRecord<byte[], byte[]>> producedRecordsList =
            kafkaTestUtils.produceRecords(numberOfRecords, topicName, partitionId);

        // You can get details about what got produced into Kafka, including the partition and offset for each message.
        for (final ProducedKafkaRecord<byte[], byte[]> producedKafkaRecord: producedRecordsList) {
            // This is the key of the message that was produced.
            final String key = new String(producedKafkaRecord.getKey(), StandardCharsets.UTF_8);

            // This is the value of the message that was produced.
            final String value = new String(producedKafkaRecord.getValue(), StandardCharsets.UTF_8);

            // Other details about topic, partition, and offset it was written onto.
            final String topic = producedKafkaRecord.getTopic();
            final int partition = producedKafkaRecord.getPartition();
            final long offset = producedKafkaRecord.getOffset();

            // for debugging
            logger.info("Produced into topic:{} partition:{} offset:{} key:{} value:{}", topic, partition, offset, key, value);
        }

        // Now to consume all records from partition 2 only.
        final List<ConsumerRecord<String, String>> consumerRecords = kafkaTestUtils.consumeAllRecordsFromTopic(
            topicName,
            Collections.singleton(partitionId),
            StringDeserializer.class,
            StringDeserializer.class
        );

        // Validate
        assertEquals(numberOfRecords, consumerRecords.size(), "Should have 10 records");

        final Iterator<ConsumerRecord<String, String>> consumerRecordIterator = consumerRecords.iterator();
        final Iterator<ProducedKafkaRecord<byte[], byte[]>> producedKafkaRecordIterator = producedRecordsList.iterator();

        while (consumerRecordIterator.hasNext()) {
            final ConsumerRecord<String, String> consumerRecord = consumerRecordIterator.next();
            final ProducedKafkaRecord<byte[], byte[]> producedKafkaRecord = producedKafkaRecordIterator.next();

            final String expectedKey = new String(producedKafkaRecord.getKey(), StandardCharsets.UTF_8);
            final String expectedValue = new String(producedKafkaRecord.getValue(), StandardCharsets.UTF_8);
            final String actualKey = consumerRecord.key();
            final String actualValue = consumerRecord.value();

            // Make sure they match
            assertEquals(producedKafkaRecord.getTopic(), consumerRecord.topic(), "Has correct topic");
            assertEquals(producedKafkaRecord.getPartition(), consumerRecord.partition(), "Has correct partition");
            assertEquals(producedKafkaRecord.getOffset(), consumerRecord.offset(), "Has correct offset");
            assertEquals(expectedKey, actualKey, "Has correct key");
            assertEquals(expectedValue, actualValue, "Has correct value");
        }
    }

    /**
     * Test if we create a topic more than once, no errors occur.
     */
    @Test
    void testCreatingTopicMultipleTimes() {
        final String myTopic = "myTopic" + System.currentTimeMillis();
        for (int creationCounter = 0; creationCounter < 5; creationCounter++) {
            getKafkaTestUtils().createTopic(myTopic, 1, (short) 1);
        }

        final Set<String> topicNames = getKafkaTestUtils().getTopicNames();
        Assertions.assertTrue(topicNames.contains(myTopic));
    }

    /**
     * Simple accessor.
     */
    private KafkaTestUtils getKafkaTestUtils() {
        return sharedKafkaTestResource.getKafkaTestUtils();
    }
}
