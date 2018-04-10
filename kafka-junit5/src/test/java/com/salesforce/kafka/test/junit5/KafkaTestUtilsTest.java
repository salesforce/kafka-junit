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

import com.google.common.base.Charsets;
import com.salesforce.kafka.test.KafkaTestServer;
import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.ProducedKafkaRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Serves both as a test for the Utilities, but also as a good example of how to use them.
 */
@ExtendWith(KafkaResourceExtension.class)
public class KafkaTestUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTestUtilsTest.class);

    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     */
    private final SharedKafkaTestResource sharedKafkaTestResource;

    /**
     * Before every test, we generate a random topic name and create it within the embedded kafka server.
     * Each test can then be segmented run any tests against its own topic.
     */
    private String topicName;

    /**
     * Constructor where KafkaResourceExtension provides the sharedKafkaTestResource object.
     * @param sharedKafkaTestResource Provided by KafkaResourceExtension.
     */
    public KafkaTestUtilsTest(SharedKafkaTestResource sharedKafkaTestResource) {
        this.sharedKafkaTestResource = sharedKafkaTestResource;
    }

    /**
     * This happens once before every test method.
     * Create a new empty namespace with randomly generated name.
     */
    @BeforeEach
    void beforeTest() {
        // Generate topic name
        topicName = getClass().getSimpleName() + Clock.systemUTC().millis();

        // Create topic with 3 partitions,
        // NOTE: This will create partition ids 0 thru 2, because partitions are indexed at 0 :)
        getKafkaTestServer().createTopic(topicName, 3);
    }

    /**
     * Simple smoke test and example of how to use this Utility class.
     */
    @Test
    void testProducerAndConsumerUtils() {
        final int numberOfRecords = 10;
        final int partitionId = 2;

        // Create our utility class
        final KafkaTestUtils kafkaTestUtils = new KafkaTestUtils(getKafkaTestServer());

        // Produce some random records
        final List<ProducedKafkaRecord<byte[], byte[]>> producedRecordsList =
            kafkaTestUtils.produceRecords(numberOfRecords, topicName, partitionId);

        // You can get details about what get produced
        for (ProducedKafkaRecord<byte[], byte[]> producedKafkaRecord: producedRecordsList) {
            final String key = new String(producedKafkaRecord.getKey(), Charsets.UTF_8);
            final String value = new String(producedKafkaRecord.getValue(), Charsets.UTF_8);
            final String topic = producedKafkaRecord.getTopic();
            final int partition = producedKafkaRecord.getPartition();
            final long offset = producedKafkaRecord.getOffset();

            // for debugging
            logger.info("Produced into topic:{} partition:{} offset:{} key:{} value:{}", topic, partition, offset, key, value);
        }

        // Now we'll try to consume these records.
        final List<ConsumerRecord<byte[], byte[]>> consumerRecords = kafkaTestUtils.consumeAllRecordsFromTopic(topicName);

        // Validate
        assertEquals(numberOfRecords, consumerRecords.size(), "Should have 10 records");

        final Iterator<ConsumerRecord<byte[], byte[]>> consumerRecordIterator = consumerRecords.iterator();
        final Iterator<ProducedKafkaRecord<byte[], byte[]>> producedKafkaRecordIterator = producedRecordsList.iterator();

        while (consumerRecordIterator.hasNext()) {
            ConsumerRecord<byte[], byte[]> consumerRecord = consumerRecordIterator.next();
            ProducedKafkaRecord<byte[], byte[]> producedKafkaRecord = producedKafkaRecordIterator.next();

            final String expectedKey = new String(producedKafkaRecord.getKey(), Charsets.UTF_8);
            final String expectedValue = new String(producedKafkaRecord.getValue(), Charsets.UTF_8);
            final String actualKey = new String(consumerRecord.key(), Charsets.UTF_8);
            final String actualValue = new String(consumerRecord.value(), Charsets.UTF_8);

            // Make sure they match
            assertEquals(producedKafkaRecord.getTopic(), consumerRecord.topic(), "Has correct topic");
            assertEquals(producedKafkaRecord.getPartition(), consumerRecord.partition(), "Has correct partition");
            assertEquals(producedKafkaRecord.getOffset(), consumerRecord.offset(), "Has correct offset");
            assertEquals(expectedKey, actualKey, "Has correct key");
            assertEquals(expectedValue, actualValue, "Has correct value");
        }
    }

    /**
     * Simple accessor.
     */
    private KafkaTestServer getKafkaTestServer() {
        return sharedKafkaTestResource.getKafkaTestServer();
    }
}