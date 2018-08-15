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

/**
 * Contains information about a single Kafka broker within a cluster.
 * Provides accessors to get connection information for a specific broker, as well as
 * the ability to individually start/stop a specific Broker.
 */
public class KafkaBroker {

    private final KafkaTestServer kafkaTestServer;

    /**
     * Constructor.
     * @param kafkaTestServer Internal KafkaTestServer instance.
     */
    public KafkaBroker(final KafkaTestServer kafkaTestServer) {
        this.kafkaTestServer = kafkaTestServer;
    }

    public int getBrokerId() {
        return kafkaTestServer.getBrokerId();
    }

    /**
     * bootstrap.servers string to configure Kafka consumers or producers to access the Kafka cluster.
     * @return Connect string to use for Kafka clients.
     */
    public String getConnectString() {
        return kafkaTestServer.getKafkaConnectString();
    }

    /**
     * Starts the Kafka broker.
     * @throws Exception on startup errors.
     */
    public void start() throws Exception {
        kafkaTestServer.start();
    }

    /**
     * Stop/shutdown Kafka broker.
     * @throws Exception on shutdown errors.
     */
    public void stop() throws Exception {
        kafkaTestServer.stop();
    }

    @Override
    public String toString() {
        return "KafkaBroker{"
            + "brokerId=" + getBrokerId()
            + ", connectString='" + getConnectString() + '\''
            + '}';
    }
}