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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Validation tests against KafkaTestServer class.
 */
class KafkaTestServerTest {

    /**
     * Integration test validates that we can use transactional consumers and producers against the Test kafka instance.
     */
    @Test
    void testExactlyOnceTransaction() throws Exception {
        // Define topic to test with.
        final String theTopic = "transactional-topic" + System.currentTimeMillis();

        // Create our test server instance.
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Start it and create our topic.
            kafkaTestServer.start();

            // Create test utils instance.
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestServer);

            // Create a topic.
            kafkaTestUtils.createTopic(theTopic, 1, (short) 1);

            // Define override properties.
            Properties config = new Properties();
            config.put("group.id", "test-consumer-group");
            config.put("enable.auto.commit", "false");
            config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            config.put("auto.offset.reset", "earliest");

            try (final KafkaConsumer<String, String> consumer
                = kafkaTestUtils.getKafkaConsumer(StringDeserializer.class, StringDeserializer.class, config)) {

                // Subscribe to the topic
                consumer.subscribe(Collections.singletonList(theTopic));

                // Setup the producer
                config = new Properties();
                config.put("transactional.id", "MyRandomString" + System.currentTimeMillis());

                try (final KafkaProducer<String, String> producer
                    = kafkaTestUtils.getKafkaProducer(StringSerializer.class, StringSerializer.class, config)) {
                    // Init transaction and begin
                    producer.initTransactions();
                    producer.beginTransaction();

                    // Define our test message and key
                    final String theKey = "Here is the Key";
                    final String theMsg = "Here is the message";
                    final ProducerRecord<String, String> r = new ProducerRecord<>(theTopic, theKey, theMsg);

                    // Send and commit the record.
                    producer.send(r);
                    producer.commitTransaction();

                    // Use consumer to read the message
                    final ConsumerRecords<String, String> records = consumer.poll(5000);
                    Assertions.assertFalse(records.isEmpty(), "Should not be empty!");
                    Assertions.assertEquals(1, records.count(), "Should have a single record");
                    for (final ConsumerRecord<String, String> record : records) {
                        Assertions.assertEquals(theKey, record.key(), "Keys should match");
                        Assertions.assertEquals(theMsg, record.value(), "Values should match");
                        consumer.commitSync();
                    }
                }
            }
        }
    }

    /**
     * Test consuming using subscribe.
     */
    @Test
    void testProducerAndConsumerSubscribe() throws Exception {
        // Define topic to test with.
        final String theTopic = "subscribe-topic" + System.currentTimeMillis();
        final int partitionId = 0;

        // Define our message
        final String expectedKey = "my-key";
        final String expectedValue = "my test message";

        // Create our test server instance.
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Start it and create our topic.
            kafkaTestServer.start();

            // Create test utils instance.
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestServer);

            // Create a topic.
            kafkaTestUtils.createTopic(theTopic, 1, (short) 1);

            // Define override properties.
            final Properties config = new Properties();
            config.put("group.id", "test-consumer-group");
            config.put("enable.auto.commit", "false");
            config.put("auto.offset.reset", "earliest");

            // Define the record we want to produce
            final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(theTopic, partitionId, expectedKey, expectedValue);

            // Create a new producer
            try (final KafkaProducer<String, String> producer =
                kafkaTestUtils.getKafkaProducer(StringSerializer.class, StringSerializer.class, config)) {

                // Produce it & wait for it to complete.
                final Future<RecordMetadata> future = producer.send(producerRecord);
                producer.flush();
                while (!future.isDone()) {
                    Thread.sleep(500L);
                }
            }

            // Create a consumer
            try (final KafkaConsumer<String, String> consumer =
                kafkaTestUtils.getKafkaConsumer(StringDeserializer.class, StringDeserializer.class, config)) {

                // Subscribe to the topic.
                consumer.subscribe(Collections.singleton(theTopic));

                // Consume messages.
                ConsumerRecords<String, String> records = consumer.poll(1000L);

                // poll() doesn't make any promises that it will return records.  On first poll() it does
                // several async background requests, so try up to 10 times.
                if (records.isEmpty()) {
                    for (int attempts = 0; attempts < 10; attempts++) {
                        records = consumer.poll(1000L);

                        // If we got any records
                        if (!records.isEmpty()) {
                            // Break out of loop
                            break;
                        }
                    }
                }

                // Validate we got our message
                Assertions.assertEquals(1, records.count(), "Should have 1 record");
                for (final ConsumerRecord<String, String> record : records) {
                    Assertions.assertEquals(expectedKey, record.key(), "Keys should match");
                    Assertions.assertEquals(expectedValue, record.value(), "Values should match");
                }
            }
        }
    }

    /**
     * Integration test validates that we can override broker properties.
     */
    @Test
    void testOverrideBrokerProperties() throws Exception {
        final int expectedBrokerId = 22;

        // Define our override property
        final Properties overrideProperties = new Properties();
        overrideProperties.put("broker.id", String.valueOf(expectedBrokerId));
        
        // Create our test server instance passing override properties.
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer(overrideProperties)) {
            // Lets try to be sneaky and change our local property after calling the constructor.
            // This shouldn't have any effect on the properties already passed into the constructor.
            overrideProperties.put("broker.id", "1000");

            // Start service
            kafkaTestServer.start();

            // Ask the instance for its brokerId.
            Assertions.assertEquals(expectedBrokerId, kafkaTestServer.getBrokerId());

            // Ask the cluster for the node's brokerId.
            // Create test utils instance.
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestServer);

            // Get details about nodes in the cluster.
            final List<Node> nodesInCluster = kafkaTestUtils.describeClusterNodes();

            // Sanity test
            Assertions.assertEquals(1, nodesInCluster.size(), "Should only have a single node");

            // Get details about our test broker/node
            final Node node = nodesInCluster.get(0);

            // Validate
            Assertions.assertEquals(expectedBrokerId, node.id(), "Has expected overridden broker Id");
        }
    }

    /**
     * Test the getKafkaBrokers() getter method before the broker is started.
     */
    @Test
    void testGetKafkaBrokersBeforeBrokerIsStarted() throws Exception {
        // Create our test server instance
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Ask for brokers before starting
            Assertions.assertThrows(IllegalStateException.class, kafkaTestServer::getKafkaBrokers);
        }
    }

    /**
     * Test the getKafkaBrokers() getter method.
     */
    @Test
    void testGetKafkaBrokers() throws Exception {
        final int expectedBrokerId = 1;
        // Create our test server instance
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Start broker
            kafkaTestServer.start();

            final KafkaBrokers kafkaBrokers = kafkaTestServer.getKafkaBrokers();
            Assertions.assertNotNull(kafkaBrokers, "Should not be null.");
            Assertions.assertEquals(1, kafkaBrokers.size(), "Should have 1 broker in list.");

            final KafkaBroker broker = kafkaBrokers.getBrokerById(expectedBrokerId);
            Assertions.assertNotNull(broker);
            Assertions.assertEquals(kafkaTestServer.getKafkaConnectString(), broker.getConnectString());
            Assertions.assertEquals(expectedBrokerId, broker.getBrokerId());
        }
    }

    /**
     * Tests restarting the instance.  This validates you can restart a broker and it comes back up in a sane manner containing
     * all the same data it contained prior to being shut down.
     *
     * This test does the following:
     *      - Start broker
     *      - Create a topic
     *      - Produce 2 records into topic
     *      - Stops broker
     *      - Starts broker
     *      - Consumes messages from broker
     */
    @Test
    void testRestartingBroker() throws Exception {
        final String topicName = "testRestartingBroker-" + System.currentTimeMillis();
        final int expectedMsgCount = 2;

        // Create our test server instance
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Start broker
            kafkaTestServer.start();

            // Create KafkaTestUtils
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestServer);

            // Create topic
            kafkaTestUtils.createTopic(topicName, 1, (short) 1);

            // Publish 2 messages into topic
            kafkaTestUtils.produceRecords(expectedMsgCount, topicName, 0);

            // Sanity test - Consume the messages back out before shutting down broker.
            List<ConsumerRecord<byte[], byte[]>> records = kafkaTestUtils.consumeAllRecordsFromTopic(topicName);
            Assertions.assertNotNull(records);
            Assertions.assertEquals(expectedMsgCount, records.size(), "Should have found 2 records.");

            // Call stop/close on the broker
            kafkaTestServer.stop();

            // Start instance back up
            kafkaTestServer.start();

            // Attempt to consume messages after restarting service.
            records = kafkaTestUtils.consumeAllRecordsFromTopic(topicName);
            Assertions.assertNotNull(records);
            Assertions.assertEquals(expectedMsgCount, records.size(), "Should have found 2 records.");
        }
    }
}