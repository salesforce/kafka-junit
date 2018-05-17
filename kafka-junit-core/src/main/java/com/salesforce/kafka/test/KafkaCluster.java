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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Properties;

/**
 * This interface abstracts away knowing if the underlying 'kafka cluster' is a single server (KafkaTestServer)
 * or a cluster of 1 or more brokers (KafkaTestCluster).
 *
 * TODO: Perhaps this is silly tho and we should just merge KafkaTestCluster and KafkaTestServer, since a KafkaTestCluster of size 1 is
 * essentially the same thing.
 */
public interface KafkaCluster extends AutoCloseable {

    /**
     * Creates and starts ZooKeeper and Kafka server instances.
     * @throws Exception on startup errors.
     */
    void start() throws Exception;

    /**
     * Closes the internal servers. Failing to call this at the end of your tests will likely
     * result in leaking instances.
     */
    void close() throws Exception;

    /**
     * @return The proper connect string to use for Kafka.
     */
    String getKafkaConnectString();

    /**
     * @return The proper connect string to use for Zookeeper.
     */
    String getZookeeperConnectString();

    // TODO: From here down provides backwards compatibility.  Maybe we just want to instead add a getter for
    // an instance of KafkaTestUtils and allow access to these methods only via it?

    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName the namespace name to create.
     * @param partitions the number of partitions to create.
     * @param replicationFactor the number of replicas for the topic.
     */
    void createTopic(final String topicName, final int partitions, final short replicationFactor);

    /**
     * Creates a Kafka AdminClient connected to our test cluster.
     * @return Kafka AdminClient instance.
     */
    AdminClient getAdminClient();

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer
    );

    /**
     * Creates a kafka producer that is connected to our test server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @param config Additional producer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer,
        final Properties config
    );

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer
    );

    /**
     * Return Kafka Consumer configured to consume from internal Kafka Server.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @param config Additional consumer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer,
        final Properties config
    );
}
