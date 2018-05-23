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

import com.google.common.base.Charsets;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A collection of re-usable patterns for interacting with embedded Kafka server.
 */
public class KafkaTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTestUtils.class);

    // The embedded Kafka server to interact with.
    private final KafkaProvider kafkaProvider;

    /**
     * Constructor.
     * @param kafkaProvider The kafka cluster to operate on.
     */
    public KafkaTestUtils(KafkaProvider kafkaProvider) {
        if (kafkaProvider == null) {
            throw new IllegalArgumentException("KafkaCluster argument cannot be null.");
        }
        this.kafkaProvider = kafkaProvider;
    }

    /**
     * Produce some records into the defined kafka namespace.
     *
     * @param keysAndValues Records you want to produce.
     * @param topicName the namespace name to produce into.
     * @param partitionId the partition to produce into.
     * @return List of ProducedKafkaRecords.
     */
    public List<ProducedKafkaRecord<byte[], byte[]>> produceRecords(
        final Map<byte[], byte[]> keysAndValues,
        final String topicName,
        final int partitionId
    ) {
        // This holds the records we produced
        final List<ProducerRecord<byte[], byte[]>> producedRecords = new ArrayList<>();

        // This holds futures returned
        final List<Future<RecordMetadata>> producerFutures = new ArrayList<>();

        try (final KafkaProducer<byte[], byte[]> producer = getKafkaProducer(
            ByteArraySerializer.class,
            ByteArraySerializer.class,
            new Properties()
        )) {
            for (final Map.Entry<byte[], byte[]> entry: keysAndValues.entrySet()) {
                // Construct filter
                final ProducerRecord<byte[], byte[]> record
                    = new ProducerRecord<>(topicName, partitionId, entry.getKey(), entry.getValue());

                producedRecords.add(record);

                // Send it.
                producerFutures.add(producer.send(record));
            }

            // Publish to the namespace and close.
            producer.flush();
            logger.info("Produce completed");
        }

        // Loop thru the futures, and build KafkaRecord objects
        final List<ProducedKafkaRecord<byte[], byte[]>> kafkaRecords = new ArrayList<>();
        try {
            for (int x = 0; x < keysAndValues.size(); x++) {
                final RecordMetadata metadata = producerFutures.get(x).get();
                final ProducerRecord<byte[], byte[]> producerRecord = producedRecords.get(x);

                kafkaRecords.add(ProducedKafkaRecord.newInstance(metadata, producerRecord));
            }
        } catch (final InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }

        return kafkaRecords;
    }

    /**
     * Produce randomly generated records into the defined kafka namespace.
     *
     * @param numberOfRecords how many records to produce
     * @param topicName the namespace name to produce into.
     * @param partitionId the partition to produce into.
     * @return List of ProducedKafkaRecords.
     */
    public List<ProducedKafkaRecord<byte[], byte[]>> produceRecords(
        final int numberOfRecords,
        final String topicName,
        final int partitionId
    ) {
        Map<byte[], byte[]> keysAndValues = new HashMap<>();

        // Generate random & unique data
        for (int x = 0; x < numberOfRecords; x++) {
            // Construct key and value
            long timeStamp = Clock.systemUTC().millis();
            String key = "key" + timeStamp;
            String value = "value" + timeStamp;

            // Add to map
            keysAndValues.put(key.getBytes(Charsets.UTF_8), value.getBytes(Charsets.UTF_8));
        }

        return produceRecords(keysAndValues, topicName, partitionId);
    }

    /**
     * This will consume all records from all partitions on the given topic.
     * @param topic Topic to consume from.
     * @return List of ConsumerRecords consumed.
     */
    public List<ConsumerRecord<byte[], byte[]>> consumeAllRecordsFromTopic(final String topic) {
        final List<Integer> partitionIds = new ArrayList<>();

        // Connect to broker to determine what partitions are available.
        try (final KafkaConsumer<byte[], byte[]> kafkaConsumer = getKafkaConsumer(
            ByteArrayDeserializer.class,
            ByteArrayDeserializer.class,
            new Properties()
        )) {
            for (final PartitionInfo partitionInfo : kafkaConsumer.partitionsFor(topic)) {
                partitionIds.add(partitionInfo.partition());
            }
        }
        return consumeAllRecordsFromTopic(topic, partitionIds);
    }

    /**
     * This will consume all records from only the partitions given.
     * @param topic Topic to consume from.
     * @param partitionIds Collection of PartitionIds to consume.
     * @return List of ConsumerRecords consumed.
     */
    public List<ConsumerRecord<byte[], byte[]>> consumeAllRecordsFromTopic(final String topic, Collection<Integer> partitionIds) {
        // Create topic Partitions
        final List<TopicPartition> topicPartitions = new ArrayList<>();
        for (Integer partitionId: partitionIds) {
            topicPartitions.add(new TopicPartition(topic, partitionId));
        }

        // Holds our results.
        final List<ConsumerRecord<byte[], byte[]>> allRecords = new ArrayList<>();

        // Connect Consumer
        try (final KafkaConsumer<byte[], byte[]> kafkaConsumer =
            getKafkaConsumer(ByteArrayDeserializer.class, ByteArrayDeserializer.class, new Properties())) {

            // Assign topic partitions & seek to head of them
            kafkaConsumer.assign(topicPartitions);
            kafkaConsumer.seekToBeginning(topicPartitions);

            // Pull records from kafka, keep polling until we get nothing back
            ConsumerRecords<byte[], byte[]> records;
            do {
                // Grab records from kafka
                records = kafkaConsumer.poll(2000L);
                logger.info("Found {} records in kafka", records.count());

                // Add to our array list
                records.forEach(allRecords::add);

            }
            while (!records.isEmpty());
        }

        // return all records
        return allRecords;
    }

    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName the namespace name to create.
     * @param partitions the number of partitions to create.
     * @param replicationFactor the number of replicas for the topic.
     */
    public void createTopic(final String topicName, final int partitions, final short replicationFactor) {
        // Create admin client
        try (final AdminClient adminClient = getAdminClient()) {
            // Define topic
            final NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);

            // Create topic, which is async call.
            final CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singleton(newTopic));

            // Since the call is Async, Lets wait for it to complete.
            createTopicsResult.values().get(topicName).get();
        } catch (InterruptedException | ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException(e.getMessage(), e);
            }
            // TopicExistsException - Swallow this exception, just means the topic already exists.
        }
    }

    /**
     * Creates a Kafka AdminClient connected to our test server.
     * @return Kafka AdminClient instance.
     */
    public AdminClient getAdminClient() {
        return KafkaAdminClient.create(buildDefaultClientConfig());
    }

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer) {

        return getKafkaProducer(keySerializer, valueSerializer, new Properties());
    }

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @param config Additional producer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer,
        final Properties config) {

        // Build config
        final Map<String, Object> kafkaProducerConfig = new HashMap<>();
        kafkaProducerConfig.put("bootstrap.servers", kafkaProvider.getKafkaConnectString());
        kafkaProducerConfig.put("max.in.flight.requests.per.connection", 1);
        kafkaProducerConfig.put("retries", 5);
        kafkaProducerConfig.put("client.id", getClass().getSimpleName() + " Producer");
        kafkaProducerConfig.put("batch.size", 0);
        kafkaProducerConfig.put("key.serializer", keySerializer);
        kafkaProducerConfig.put("value.serializer", valueSerializer);

        // Override config
        if (config != null) {
            for (final Map.Entry<Object, Object> entry: config.entrySet()) {
                kafkaProducerConfig.put(entry.getKey().toString(), entry.getValue());
            }
        }

        // Create and return Producer.
        return new KafkaProducer<>(kafkaProducerConfig);
    }

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer) {

        return getKafkaConsumer(keyDeserializer, valueDeserializer, new Properties());
    }

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @param config Additional consumer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer,
        final Properties config) {

        // Build config
        Map<String, Object> kafkaConsumerConfig = buildDefaultClientConfig();
        kafkaConsumerConfig.put("key.deserializer", keyDeserializer);
        kafkaConsumerConfig.put("value.deserializer", valueDeserializer);
        kafkaConsumerConfig.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");

        // Override config
        if (config != null) {
            for (Map.Entry<Object, Object> entry: config.entrySet()) {
                kafkaConsumerConfig.put(entry.getKey().toString(), entry.getValue());
            }
        }

        // Create and return Consumer.
        return new KafkaConsumer<>(kafkaConsumerConfig);
    }

    /**
     * Internal helper method to build a default configuration.
     */
    private Map<String, Object> buildDefaultClientConfig() {
        final Map<String, Object> defaultClientConfig = new HashMap<>();
        defaultClientConfig.put("bootstrap.servers", kafkaProvider.getKafkaConnectString());
        defaultClientConfig.put("client.id", "test-consumer-id");
        return defaultClientConfig;
    }
}
