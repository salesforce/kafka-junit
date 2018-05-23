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

import com.google.common.collect.Iterables;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

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
     * Integration test validates that we can override broker properties.
     */
    @Test
    void testOverrideBrokerProperties() throws Exception {
        final String expectedBrokerId = "22";

        // Define our override property
        final Properties overrideProperties = new Properties();
        overrideProperties.put("broker.id", expectedBrokerId);
        
        // Create our test server instance passing override properties.
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer(overrideProperties)) {
            // Lets try to be sneaky and change our local property after calling the constructor.
            // This shouldn't have any effect on the properties already passed into the constructor.
            overrideProperties.put("broker.id", "1000");

            // Start service
            kafkaTestServer.start();

            // Create test utils instance.
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestServer);

            // Create an AdminClient
            try (final AdminClient adminClient = kafkaTestUtils.getAdminClient()) {
                // Describe details about the cluster
                final DescribeClusterResult result = adminClient.describeCluster();

                // Get details about the nodes
                final Collection<Node> nodes = result.nodes().get();

                // Sanity test
                Assertions.assertEquals(1, nodes.size(), "Should only have a single node");

                // Get details about our test broker/node
                final Node node = Iterables.get(nodes, 0);

                // Validate
                Assertions.assertEquals(expectedBrokerId, node.idString(), "Has expected overridden broker Id");
            }
        }
    }
}