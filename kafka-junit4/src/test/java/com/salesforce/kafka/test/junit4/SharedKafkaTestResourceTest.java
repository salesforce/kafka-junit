/**
 * Copyright (c) 2017-2021, Salesforce.com, Inc.
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

package com.salesforce.kafka.test.junit4;

import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.KafkaTestUtils;
import org.apache.kafka.common.Node;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Runs smoke tests against a PLAINTEXT enabled cluster.
 * @see AbstractSharedKafkaTestResourceTest for additional test case definitions.
 */
public class SharedKafkaTestResourceTest extends AbstractSharedKafkaTestResourceTest {

    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     *
     * This example we start a cluster with 2 brokers (defaults to a single broker) and configure the brokers to
     * disable topic auto-creation.
     */
    @ClassRule
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        // Start a cluster with 2 brokers.
        .withBrokers(2)
        // Disable topic auto-creation.
        .withBrokerProperty("auto.create.topics.enable", "false");

    /**
     * Example in a multi-broker cluster, how to stop an individual broker and bring it back on-line.
     */
    @Test
    public void testBringingBrokerOffLine() throws Exception {
        // Now we want to test behavior by stopping brokerId 2.
        final KafkaBroker broker2 = sharedKafkaTestResource
            .getKafkaBrokers()
            .getBrokerById(2);

        // Shutdown broker Id 2.
        broker2.stop();

        // It may take a moment for the broker to cleanly shut down.
        List<Node> nodes = Collections.emptyList();
        for (int attempts = 0; attempts <= 5; attempts++) {
            // Describe the cluster and wait for it to go to 1 broker.
            try {
                nodes = getKafkaTestUtils().describeClusterNodes();
                if (nodes.size() == 1) {
                    break;
                }
                Thread.sleep(1000L);
            } catch (final RuntimeException timeoutException) {
                // Swallow and retry, sometimes times out describing the cluster due to the broker going away
            }
        }

        // We should only have 1 node now, and it should not include broker Id 2.
        assertEquals(1, nodes.size());
        nodes.forEach((node) -> assertNotEquals("Should not include brokerId 2", 2, node.id()));

        // Test your applications behavior when a broker becomes unavailable or leadership changes.

        // Bring the broker back up
        broker2.start();

        // It may take a while for the broker to successfully rejoin the cluster,
        // Block until the broker has come on-line and then continue.
        getKafkaTestUtils()
            .waitForBrokerToComeOnLine(2, 10, TimeUnit.SECONDS);

        // We should have 2 nodes again.
        nodes = getKafkaTestUtils().describeClusterNodes();
        assertEquals(2, nodes.size());

        // Collect the brokerIds in the cluster.
        final Set<Integer> foundBrokerIds = nodes.stream()
            .map(Node::id)
            .collect(Collectors.toSet());

        assertTrue("Found brokerId 1", foundBrokerIds.contains(1));
        assertTrue("Found brokerId 2", foundBrokerIds.contains(2));
    }

    /**
     * Simple accessor.
     */
    protected KafkaTestUtils getKafkaTestUtils() {
        return sharedKafkaTestResource.getKafkaTestUtils();
    }

    @Override
    protected String getExpectedListenerProtocol() {
        return "PLAINTEXT";
    }
}