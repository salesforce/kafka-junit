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
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
     * Produce some records into the defined kafka topic.
     *
     * @param keysAndValues Records you want to produce.
     * @param topicName the topic to produce into.
     * @param partitionId the partition to produce into.
     * @return List of ProducedKafkaRecords.
     */
    public List<ProducedKafkaRecord<byte[], byte[]>> produceRecords(
        final Map<byte[], byte[]> keysAndValues,
        final String topicName,
        final int partitionId
    ) {
        // Defines customized producer properties.  Ensure data written to all ISRs.
        final Properties producerProperties = new Properties();
        producerProperties.put("acks", "all");

        // This holds the records we produced
        final List<ProducerRecord<byte[], byte[]>> producedRecords = new ArrayList<>();

        // This holds futures returned
        final List<Future<RecordMetadata>> producerFutures = new ArrayList<>();

        try (final KafkaProducer<byte[], byte[]> producer = getKafkaProducer(
            ByteArraySerializer.class,
            ByteArraySerializer.class,
            producerProperties
        )) {
            for (final Map.Entry<byte[], byte[]> entry: keysAndValues.entrySet()) {
                // Construct filter
                final ProducerRecord<byte[], byte[]> record
                    = new ProducerRecord<>(topicName, partitionId, entry.getKey(), entry.getValue());

                producedRecords.add(record);

                // Send it.
                producerFutures.add(producer.send(record));
            }

            // Publish to the topic and close.
            producer.flush();
            logger.debug("Produce completed");
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
     * Produce randomly generated records into the defined kafka topic.
     *
     * @param numberOfRecords how many records to produce
     * @param topicName the topic to produce into.
     * @param partitionId the partition to produce into.
     * @return List of ProducedKafkaRecords.
     */
    public List<ProducedKafkaRecord<byte[], byte[]>> produceRecords(
        final int numberOfRecords,
        final String topicName,
        final int partitionId
    ) {
        final Map<byte[], byte[]> keysAndValues = new HashMap<>();

        // Generate random & unique data
        for (int index = 0; index < numberOfRecords; index++) {
            // Construct key and value
            final long timeStamp = Clock.systemUTC().millis();
            final String key = "key" + timeStamp;
            final String value = "value" + timeStamp;

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
        return consumeAllRecordsFromTopic(topic, ByteArrayDeserializer.class, ByteArrayDeserializer.class);
    }

    /**
     * This will consume all records from only the partitions given.
     * @param topic Topic to consume from.
     * @param partitionIds Collection of PartitionIds to consume.
     * @return List of ConsumerRecords consumed.
     */
    public List<ConsumerRecord<byte[], byte[]>> consumeAllRecordsFromTopic(final String topic, Collection<Integer> partitionIds) {
        return consumeAllRecordsFromTopic(topic, partitionIds, ByteArrayDeserializer.class, ByteArrayDeserializer.class);
    }

    /**
     * This will consume all records from all partitions on the given topic.
     * @param <K> Type of key values.
     * @param <V> Type of message values.
     * @param topic Topic to consume from.
     * @param keyDeserializer How to deserialize the key values.
     * @param valueDeserializer How to deserialize the messages.
     * @return List of ConsumerRecords consumed.
     */
    public <K, V> List<ConsumerRecord<K, V>> consumeAllRecordsFromTopic(
        final String topic,
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer
    ) {
        // Find all partitions on topic.
        final TopicDescription topicDescription = describeTopic(topic);
        final Collection<Integer> partitions = topicDescription
            .partitions()
            .stream()
            .map(TopicPartitionInfo::partition)
            .collect(Collectors.toList());

        // Consume messages
        return consumeAllRecordsFromTopic(topic, partitions, keyDeserializer, valueDeserializer);
    }

    /**
     * This will consume all records from the partitions passed on the given topic.
     * @param <K> Type of key values.
     * @param <V> Type of message values.
     * @param topic Topic to consume from.
     * @param partitionIds Which partitions to consume from.
     * @param keyDeserializer How to deserialize the key values.
     * @param valueDeserializer How to deserialize the messages.
     * @return List of ConsumerRecords consumed.
     */
    public <K, V> List<ConsumerRecord<K, V>> consumeAllRecordsFromTopic(
        final String topic,
        final Collection<Integer> partitionIds,
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer
    ) {
        // Create topic Partitions
        final List<TopicPartition> topicPartitions = partitionIds
            .stream()
            .map((partitionId) -> new TopicPartition(topic, partitionId))
            .collect(Collectors.toList());

        // Holds our results.
        final List<ConsumerRecord<K, V>> allRecords = new ArrayList<>();

        // Connect Consumer
        try (final KafkaConsumer<K, V> kafkaConsumer = getKafkaConsumer(keyDeserializer, valueDeserializer, new Properties())) {

            // Assign topic partitions & seek to head of them
            kafkaConsumer.assign(topicPartitions);
            kafkaConsumer.seekToBeginning(topicPartitions);

            // Pull records from kafka, keep polling until we get nothing back
            ConsumerRecords<K, V> records;
            final int maxEmptyLoops = 2;
            int loopsLeft = maxEmptyLoops;
            do {
                // Grab records from kafka
                records = kafkaConsumer.poll(2000L);
                logger.debug("Found {} records in kafka", records.count());

                // Add to our array list
                records.forEach(allRecords::add);

                // We want two full poll() calls that return empty results to break the loop.
                if (!records.isEmpty()) {
                    // If we found records, reset our loop control variable.
                    loopsLeft = maxEmptyLoops;
                } else {
                    // Otherwise decrement the loop control variable.
                    loopsLeft--;
                }

            }
            while (loopsLeft > 0);
        }

        // return all records
        return allRecords;
    }

    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName the topic name to create.
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
     * Describes a topic in Kafka.
     * @param topicName the topic to describe.
     * @return Description of the topic.
     */
    public TopicDescription describeTopic(final String topicName) {
        // Create admin client
        try (final AdminClient adminClient = getAdminClient()) {
            // Make async call to describe the topic.
            final DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singleton(topicName));

            return describeTopicsResult.values().get(topicName).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * List all names of topics in Kafka.
     * @return Set of topics found in Kafka.
     */
    public Set<String> getTopicNames() {
        try (final AdminClient adminClient = getAdminClient()) {
            return adminClient.listTopics().names().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get information about all topics in Kafka.
     * @return Set of topics found in Kafka.
     */
    public List<TopicListing> getTopics() {
        try (final AdminClient adminClient = getAdminClient()) {
            return new ArrayList<>(adminClient.listTopics().listings().get());
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Describe nodes within Kafka cluster.
     * @return Collection of nodes within the Kafka cluster.
     */
    public List<Node> describeClusterNodes() {
        // Create admin client
        try (final AdminClient adminClient = getAdminClient()) {
            final DescribeClusterResult describeClusterResult = adminClient.describeCluster();
            return new ArrayList<>(describeClusterResult.nodes().get());
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
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
        final Class<? extends Serializer<V>> valueSerializer
    ) {

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
        final Properties config
    ) {

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
        final Class<? extends Deserializer<V>> valueDeserializer
    ) {

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
        final Properties config
    ) {

        // Build config
        final Map<String, Object> kafkaConsumerConfig = buildDefaultClientConfig();
        kafkaConsumerConfig.put("key.deserializer", keyDeserializer);
        kafkaConsumerConfig.put("value.deserializer", valueDeserializer);
        kafkaConsumerConfig.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");

        // Override config
        if (config != null) {
            for (final Map.Entry<Object, Object> entry: config.entrySet()) {
                kafkaConsumerConfig.put(entry.getKey().toString(), entry.getValue());
            }
        }

        // Create and return Consumer.
        return new KafkaConsumer<>(kafkaConsumerConfig);
    }

    /**
     * Utility method for waiting until a broker has successfully joined a cluster.
     *
     * @param brokerId The if of the broker to wait for.
     * @param timeoutDuration How long to wait before throwing a TimeoutException.
     * @param timeUnit The unit of time for how long to wait.
     * @throws TimeoutException If the broker is not online before the timeoutDuration has expired.
     */
    public void waitForBrokerToComeOnLine(final int brokerId, final long timeoutDuration, final TimeUnit timeUnit) throws TimeoutException {
        // Use system clock to calculate timeout.
        final Clock clock = Clock.systemUTC();

        // Calculate our timeout in ms.
        final long timeoutMs = clock.millis() + TimeUnit.MILLISECONDS.convert(timeoutDuration, timeUnit);

        // Start looping.
        do {
            try {
                // Ask for the nodes in the cluster.
                final Collection<Node> nodes = describeClusterNodes();

                // Look for broker
                final boolean foundBroker = nodes
                    .stream()
                    .map(Node::id)
                    .anyMatch((id) -> id == brokerId);

                // If we found the broker,
                if (foundBroker) {
                    // Good, return.
                    return;
                }

                // Small wait to throttle cycling.
                Thread.sleep(100);
            } catch (final InterruptedException exception) {
                // Caught interrupt, break out of loop.
                break;
            }
        }
        while (clock.millis() <= timeoutMs);

        // If we got here, throw timeout exception
        throw new TimeoutException("Cluster failed to come online within " + timeoutMs + " milliseconds.");
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