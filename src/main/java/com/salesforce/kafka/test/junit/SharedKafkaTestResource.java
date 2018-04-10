/**
 * Copyright (c) 2017, Salesforce.com, Inc.
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

package com.salesforce.kafka.test.junit;

import com.salesforce.kafka.test.KafkaTestServer;
import com.salesforce.kafka.test.KafkaTestUtils;
import org.apache.curator.test.TestingServer;

/**
 * Shared Kafka Test Resource instance.  Contains references to internal Kafka and Zookeeper server instances.
 */
public class SharedKafkaTestResource {
    /**
     * Our internal Kafka Test Server instance.
     */
    private final KafkaTestServer kafkaTestServer = new KafkaTestServer();

    /**
     * Cached instance of KafkaTestUtils.
     */
    private KafkaTestUtils kafkaTestUtils = null;

    /**
     * @return Shared Kafka Test server instance.
     */
    public KafkaTestServer getKafkaTestServer() {
        return kafkaTestServer;
    }

    /**
     * @return Instance of KafkaTestUtils configured and ready to go.
     */
    public KafkaTestUtils getKafkaTestUtils() {
        if (kafkaTestUtils == null) {
            kafkaTestUtils = new KafkaTestUtils(getKafkaTestServer());
        }
        return kafkaTestUtils;
    }

    /**
     * @return Shared Zookeeper test server instance.
     */
    public TestingServer getZookeeperTestServer() {
        return getKafkaTestServer().getZookeeperServer();
    }

    /**
     * @return Connection string to connect to the Zookeeper instance.
     */
    public String getZookeeperConnectString() {
        return getZookeeperTestServer().getConnectString();
    }

    /**
     * @return The proper connect string to use for Kafka.
     */
    public String getKafkaConnectString() {
        return getKafkaTestServer().getKafkaConnectString();
    }
}
