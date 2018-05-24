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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaBrokerTest {
    /**
     * Tests the start() pass through method.
     */
    @Test
    void testStart() throws Exception {
        // Create mock.
        final KafkaTestServer mockServer = createMockServer(1, "localhost:1234");

        // Build brokers
        final KafkaBroker kafkaBroker = new KafkaBroker(mockServer);

        // Call start.
        kafkaBroker.start();

        // Verify mock was called.
        verify(mockServer, times(1)).start();
    }

    /**
     * Tests the stop() pass through method.
     */
    @Test
    void testStop() throws Exception {
        // Create mock.
        final KafkaTestServer mockServer = createMockServer(1, "localhost:1234");

        // Build brokers
        final KafkaBroker kafkaBroker = new KafkaBroker(mockServer);

        // Call stop.
        kafkaBroker.stop();

        // Verify mock was called.
        verify(mockServer, times(1)).stop();
    }

    /**
     * Tests the getBrokerId() pass through method.
     */
    @Test
    void testGetBrokerId() {
        final int brokerId = 1;
        final String connectString = "localhost:12345";

        // Create mock.
        final KafkaTestServer mockServer = createMockServer(brokerId, connectString);

        // Build brokers
        final KafkaBroker kafkaBroker = new KafkaBroker(mockServer);

        Assertions.assertEquals(brokerId, kafkaBroker.getBrokerId());

        // Verify mock was called.
        verify(mockServer, times(1)).getBrokerId();
    }

    /**
     * Tests the getConnectString() pass through method.
     */
    @Test
    void testGetConnectString() {
        final int brokerId = 1;
        final String connectString = "localhost:12345";

        // Create mock.
        final KafkaTestServer mockServer = createMockServer(brokerId, connectString);

        // Build brokers
        final KafkaBroker kafkaBroker = new KafkaBroker(mockServer);

        Assertions.assertEquals(connectString, kafkaBroker.getConnectString());

        // Verify mock was called.
        verify(mockServer, times(1)).getKafkaConnectString();
    }

    /**
     * Helper method to build a mock KafkaTestServer instance.
     * @param brokerId the brokerId to setup mock to use.
     * @return mock KafkaTestServer instance.
     */
    private KafkaTestServer createMockServer(final int brokerId, final String connectString) {
        // Create mocks
        final KafkaTestServer mockServer = mock(KafkaTestServer.class);
        when(mockServer.getBrokerId()).thenReturn(brokerId);
        when(mockServer.getKafkaConnectString()).thenReturn(connectString);

        return mockServer;
    }
}