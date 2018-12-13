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

package com.salesforce.kafka.test.junit4;

import com.salesforce.kafka.test.KafkaTestUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Abstract base test class.  This defines shared test cases used by other Concrete tests.
 */
public abstract class AbstractSharedKafkaTestResourceTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSharedKafkaTestResourceTest.class);

    /**
     * Validate that we started 2 brokers.
     */
    @Test
    public void testTwoBrokersStarted() {
        final Collection<Node> nodes = getKafkaTestUtils().describeClusterNodes();
        assertNotNull("Sanity test, should not be null", nodes);
        assertEquals("Should have two entries", 2, nodes.size());

        // Grab id for each node found.
        final Set<Integer> foundBrokerIds = nodes.stream()
            .map(Node::id)
            .collect(Collectors.toSet());

        assertEquals("Found 2 brokers.", 2, foundBrokerIds.size());
        assertTrue("Found brokerId 1", foundBrokerIds.contains(1));
        assertTrue("Found brokerId 2", foundBrokerIds.contains(2));
    }

    /**
     * Test consuming and producing via KafkaProducer and KafkaConsumer instances.
     */
    @Test
    public void testProducerAndConsumer() throws Exception {
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
                    assertEquals("Key matches expected", expectedKey, record.key());
                    assertEquals("value matches expected", expectedValue, record.value());
                }
            }
            while (!records.isEmpty());
        }
    }

    /**
     * Simple smoke test to ensure broker running appropriate listeners.
     */
    @Test
    public void validateListener() throws ExecutionException, InterruptedException {
        try (final AdminClient adminClient  = getKafkaTestUtils().getAdminClient()) {
            final ConfigResource broker1Resource = new ConfigResource(ConfigResource.Type.BROKER, "1");

            // Pull broker configs
            final Config configResult = adminClient
                .describeConfigs(Collections.singletonList(broker1Resource))
                .values()
                .get(broker1Resource)
                .get();

            // Check listener
            final String actualListener = configResult.get("listeners").value();
            assertTrue(
                "Expected " + getExpectedListenerProtocol() + ":// and found: " + actualListener,
                actualListener.contains(getExpectedListenerProtocol() + "://")
            );

            // Check inter broker protocol
            final String actualBrokerProtocol = configResult.get("security.inter.broker.protocol").value();
            assertEquals(
                "Unexpected inter-broker protocol",
                getExpectedListenerProtocol(),
                actualBrokerProtocol
            );
        }
    }

    /**
     * Simple accessor.
     */
    protected abstract KafkaTestUtils getKafkaTestUtils();

    /**
     * The listener protocol the test is running over.
     * @return Expected listener protocol
     */
    protected abstract String getExpectedListenerProtocol();
}
