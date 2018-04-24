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
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Properties;

/**
 * Validation tests against KafkaTestServer class.
 */
public class KafkaTestServerTest {
    /**
     * Integration test validates that we can use transactional consumers and producers against the Test kafka instance.
     * Note - This test only runs on kafka versions >= 1.1.0.  This test will be skipped on earlier kafka versions.
     */
    @Test
    void testExactlyOnceTransaction() throws Exception {
        // Define topic to test with.
        final String theTopic = "transactional-topic" + System.currentTimeMillis();

        // Create our test server instance.
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Start it and create our topic.
            kafkaTestServer.start();
            kafkaTestServer.createTopic(theTopic, 1);

            // Define override properties.
            Properties config = new Properties();
            config.put("group.id", "test-consumer-group");
            config.put("enable.auto.commit", "false");
            config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            config.put("auto.offset.reset", "earliest");

            try (final KafkaConsumer<String, String> consumer
                     = kafkaTestServer.getKafkaConsumer(StringDeserializer.class, StringDeserializer.class, config)) {
                consumer.subscribe(Collections.singletonList(theTopic));

                // Setup the producer
                config = new Properties();
                config.put("transactional.id", "MyRandomString" + System.currentTimeMillis());

                try (final KafkaProducer<String, String> producer
                         = kafkaTestServer.getKafkaProducer(StringSerializer.class, StringSerializer.class, config)) {
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
}