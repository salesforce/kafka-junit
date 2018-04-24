package com.salesforce.kafka.test;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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

            Properties config = new Properties();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestServer.getKafkaConnectString());
            config.put("group.id", "test-consumer-group");
            config.put("enable.auto.commit", "false");
            config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            config.put("auto.offset.reset", "earliest");
            config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

            try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(config)) {
                consumer.subscribe(Arrays.asList(theTopic));

                //setup the producer
                config = new Properties();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestServer.getKafkaConnectString());
                config.put("transactional.id", "MyRandomString" + System.currentTimeMillis());
                config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                try (final KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {
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