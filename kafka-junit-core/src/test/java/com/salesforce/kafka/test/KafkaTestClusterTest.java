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
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class KafkaTestClusterTest {
    /**
     * This test shows how you can run multiple brokers.
     */
    @Test
    void testMultipleBrokersClass() throws Exception {
        final int numberOfBrokers = 2;
        final String topicName = "MultiBrokerTest2-" + System.currentTimeMillis();

        try (final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(numberOfBrokers)) {
            // Start the cluster
            kafkaTestCluster.start();

            // Create test utils instance.
            final KafkaTestUtils testUtils = new KafkaTestUtils(kafkaTestCluster);

            // Define a new topic with 2 partitions, with replication factor of 2.
            final NewTopic newTopic = new NewTopic(topicName, numberOfBrokers, (short) numberOfBrokers);

            // Attempt to create a topic
            try (final AdminClient adminClientBroker1 = testUtils.getAdminClient()) {
                adminClientBroker1
                    .createTopics(Collections.singletonList(newTopic))
                    .all()
                    .get();

                // Lets describe the topic.
                final TopicDescription topicDescription = adminClientBroker1
                    .describeTopics(Collections.singleton(topicName))
                    .values()
                    .get(topicName)
                    .get();

                // Validate has 2 partitions
                Assertions.assertEquals(numberOfBrokers, topicDescription.partitions().size(), "Correct number of partitions.");

                // Validate the partitions have 2 replicas
                for (final TopicPartitionInfo topicPartitionInfo : topicDescription.partitions()) {
                    Assertions.assertEquals(numberOfBrokers, topicPartitionInfo.replicas().size(), "Should have 2 replicas");
                    Assertions.assertEquals(numberOfBrokers, topicPartitionInfo.isr().size(), "Should have 2 In-Sync-Replicas");
                }
            }
        }
    }
}