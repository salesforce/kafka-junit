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

package com.salesforce.kafka.test.kafka_1_0_x;

import com.salesforce.kafka.test.KafkaTestServer;
import com.salesforce.kafka.test.KafkaTestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smoke tests for Streams against Kafka 1.0.x and up.
 */
class StreamsBuilderSmokeTest {
    /**
     * Integration test validates that streams can be used against KafkaTestServer.
     */
    @Test
    void testStreamConsumer() throws Exception {
        // Define topic to test with.
        final String inputTopic = "stream-input-topic" + System.currentTimeMillis();
        final String outputTopic = "stream-output-topic" + System.currentTimeMillis();

        // Define how many records
        final int numberOfRecords = 25;
        final int partitionId = 0;

        // Tracks how many records the Stream consumer has processed.
        final AtomicInteger recordCounter = new AtomicInteger(0);

        // Create our test server instance.
        try (final KafkaTestServer kafkaTestServer = new KafkaTestServer()) {
            // Start it and create our topic.
            kafkaTestServer.start();

            // Create test utils instance.
            final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(kafkaTestServer);

            // Create topics
            kafkaTestUtils.createTopic(inputTopic, 1, (short) 1);
            kafkaTestUtils.createTopic(outputTopic, 1, (short) 1);

            // Produce random data into input topic
            kafkaTestUtils.produceRecords(numberOfRecords, inputTopic, partitionId);

            // Define stream consumer properties.
            final Properties config = new Properties();
            config.put(StreamsConfig.APPLICATION_ID_CONFIG, "testStreamProcessor");
            config.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestServer.getKafkaConnectString());
            config.put("group.id", "test-stream-group");
            config.put("auto.offset.reset", "earliest");

            // Serialization definition.
            final Serde<String> stringSerde = Serdes.String();

            // Build the stream
            final StreamsBuilder streamsBuilder = new StreamsBuilder();
            streamsBuilder
                // Read from input topic.
                .stream(inputTopic)

                // For each record processed, increment our counter
                .map((key, word) -> {
                    recordCounter.incrementAndGet();
                    return new KeyValue<>(word, word);
                })

                // Write to output topic.
                .to(outputTopic);

            // Create stream
            final KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), new StreamsConfig(config));
            try {
                // Start the stream consumer
                kafkaStreams.start();

                // Since stream processing is async, we need to wait for the Stream processor to start, consume messages
                // from the input topic, and process them. We'll wait for Wait for it to do its thing up to 10 seconds.
                for (int timeoutCounter = 0; timeoutCounter <= 10; timeoutCounter++) {
                    // If we've processed all of our records
                    if (recordCounter.get() >= numberOfRecords) {
                        // Break out of sleep loop.
                        break;
                    }
                    // Otherwise, we need to wait longer, sleep 1 second.
                    Thread.sleep(1000L);
                }
            } finally {
                // Close the stream consumer.
                kafkaStreams.close();
            }

            // Validation.
            Assertions.assertEquals(numberOfRecords, recordCounter.get(), "Should have 25 records processed");

            // Consume records from output topic.
            final List<ConsumerRecord<String, String>> outputRecords =
                kafkaTestUtils.consumeAllRecordsFromTopic(outputTopic, StringDeserializer.class, StringDeserializer.class);

            // Validate we got the correct number of records.
            Assertions.assertEquals(numberOfRecords, outputRecords.size());
        }
    }
}
