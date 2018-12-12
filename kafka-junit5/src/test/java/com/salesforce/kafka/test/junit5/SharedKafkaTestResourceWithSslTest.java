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

import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.listeners.SslListener;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * NOTE: This test assumes you've run the script/generateCertificatesForTest.sh script.
 *
 * Runs smoke tests against an SSL enabled cluster.
 * @see AbstractSharedKafkaTestResourceTest for test case definitions.
 */
class SharedKafkaTestResourceWithSslTest extends AbstractSharedKafkaTestResourceTest {
    /**
     * We have two node embedded kafka cluster that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     *
     * This example we start a cluster with
     *  - 2 brokers (defaults to a single broker)
     *  - configure the brokers to disable topic auto-creation.
     *  - enables SSL authentication with a test/dummy key and trust stores.
     */
    @RegisterExtension
    static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        // Start a cluster with 2 brokers.
        .withBrokers(2)
        // Disable topic auto-creation.
        .withBrokerProperty("auto.create.topics.enable", "false")
        // Register and configure SSL authentication on cluster.
        .registerListener(new SslListener()
        .withClientAuthRequested()
        .withKeyStoreLocation(SharedKafkaTestResourceWithSslTest.class.getClassLoader().getResource("kafka.keystore.jks").getFile())
        .withKeyStorePassword("password")
        .withTrustStoreLocation(SharedKafkaTestResourceWithSslTest.class.getClassLoader().getResource("kafka.truststore.jks").getFile())
        .withTrustStorePassword("password")
        .withKeyPassword("password")
    );

    /**
     * Simple accessor.
     */
    protected KafkaTestUtils getKafkaTestUtils() {
        return sharedKafkaTestResource.getKafkaTestUtils();
    }
}
