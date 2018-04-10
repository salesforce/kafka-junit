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

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Class that wraps relevant information about data that was published to kafka.
 * Used within our tests.
 *
 * @param <K> - Object type of the Key written to kafka.
 * @param <V> - Object type of the Value written to kafka.
 */
public class ProducedKafkaRecord<K, V> {
    private final String topic;
    private final int partition;
    private final long offset;
    private final K key;
    private final V value;

    /**
     * Constructor.
     *
     * @param topic Topic that the record was published to.
     * @param partition Partition that the record was published to.
     * @param offset Offset that the record was published to.
     * @param key The key that was published.
     * @param value The message value that was published.
     */
    ProducedKafkaRecord(String topic, int partition, long offset, K key, V value) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

    /**
     * Utility factory.
     * @param recordMetadata Metadata about the produced record.
     * @param producerRecord The original record that was produced.
     * @param <K> Type of key
     * @param <V> Type of message
     * @return A ProducedKafkaRecord that represents metadata about the original record, and the results of it being published.
     */
    static <K,V> ProducedKafkaRecord<K,V> newInstance(
        final RecordMetadata recordMetadata,
        final ProducerRecord<K,V> producerRecord) {
        return new ProducedKafkaRecord<>(
            recordMetadata.topic(),
            recordMetadata.partition(),
            recordMetadata.offset(),
            producerRecord.key(),
            producerRecord.value()
        );
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ProducedKafkaRecord{"
            + "topic='" + topic + '\''
            + ", partition=" + partition
            + ", offset=" + offset
            + ", key=" + key
            + ", value=" + value
            + '}';
    }
}
