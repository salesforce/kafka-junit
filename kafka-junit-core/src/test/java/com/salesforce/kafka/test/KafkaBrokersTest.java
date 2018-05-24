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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaBrokersTest {

    /**
     * Tests the getBrokerById.
     */
    @Test
    void testGetBrokerById() {
        // Create mocks
        final KafkaTestServer mockServer1 = mock(KafkaTestServer.class);
        when(mockServer1.getBrokerId()).thenReturn(1);

        final KafkaTestServer mockServer2 = mock(KafkaTestServer.class);
        when(mockServer2.getBrokerId()).thenReturn(2);

        final KafkaTestServer mockServer3 = mock(KafkaTestServer.class);
        when(mockServer3.getBrokerId()).thenReturn(3);

        final KafkaTestServer mockServer5 = mock(KafkaTestServer.class);
        when(mockServer5.getBrokerId()).thenReturn(5);

        // Build brokers
        final KafkaBroker broker1 = new KafkaBroker(mockServer1);
        final KafkaBroker broker2 = new KafkaBroker(mockServer2);
        final KafkaBroker broker3 = new KafkaBroker(mockServer3);
        final KafkaBroker broker5 = new KafkaBroker(mockServer5);
        final List<KafkaBroker> brokerList = new ArrayList<>();
        brokerList.add(broker2);
        brokerList.add(broker1);
        brokerList.add(broker5);
        brokerList.add(broker3);

        // Create KafkaBrokers instance.
        final KafkaBrokers kafkaBrokers = new KafkaBrokers(brokerList);

        // Validate
        Assertions.assertSame(broker1, kafkaBrokers.getBrokerById(1));
        Assertions.assertSame(broker2, kafkaBrokers.getBrokerById(2));
        Assertions.assertSame(broker3, kafkaBrokers.getBrokerById(3));
        Assertions.assertSame(broker5, kafkaBrokers.getBrokerById(5));

        // Attempt to get invalid brokerId
        Assertions.assertThrows(IllegalArgumentException.class, () -> kafkaBrokers.getBrokerById(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> kafkaBrokers.getBrokerById(4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> kafkaBrokers.getBrokerById(6));
    }

    /**
     * Tests the asList() method.
     */
    @Test
    void testAsList() {
        // Create mocks
        final KafkaTestServer mockServer1 = mock(KafkaTestServer.class);
        when(mockServer1.getBrokerId()).thenReturn(1);

        final KafkaTestServer mockServer2 = mock(KafkaTestServer.class);
        when(mockServer2.getBrokerId()).thenReturn(2);

        final KafkaTestServer mockServer3 = mock(KafkaTestServer.class);
        when(mockServer3.getBrokerId()).thenReturn(3);

        final KafkaTestServer mockServer5 = mock(KafkaTestServer.class);
        when(mockServer5.getBrokerId()).thenReturn(5);

        // Build brokers
        final KafkaBroker broker1 = new KafkaBroker(mockServer1);
        final KafkaBroker broker2 = new KafkaBroker(mockServer2);
        final KafkaBroker broker3 = new KafkaBroker(mockServer3);
        final KafkaBroker broker5 = new KafkaBroker(mockServer5);
        final List<KafkaBroker> brokerList = new ArrayList<>();
        brokerList.add(broker2);
        brokerList.add(broker1);
        brokerList.add(broker5);
        brokerList.add(broker3);

        // Create KafkaBrokers instance.
        final KafkaBrokers kafkaBrokers = new KafkaBrokers(brokerList);

        // Validate
        final List<KafkaBroker> result = kafkaBrokers.asList();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.size());

        Assertions.assertTrue(result.contains(broker1));
        Assertions.assertTrue(result.contains(broker2));
        Assertions.assertTrue(result.contains(broker3));
        Assertions.assertTrue(result.contains(broker5));

        // Validate list is immutable.
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    /**
     * Tests the size() method.
     */
    @Test
    void testSize() {
        // Create mocks
        final KafkaTestServer mockServer1 = mock(KafkaTestServer.class);
        when(mockServer1.getBrokerId()).thenReturn(1);

        final KafkaTestServer mockServer2 = mock(KafkaTestServer.class);
        when(mockServer2.getBrokerId()).thenReturn(2);

        final KafkaTestServer mockServer3 = mock(KafkaTestServer.class);
        when(mockServer3.getBrokerId()).thenReturn(3);

        final KafkaTestServer mockServer5 = mock(KafkaTestServer.class);
        when(mockServer5.getBrokerId()).thenReturn(5);

        // Build brokers
        final KafkaBroker broker1 = new KafkaBroker(mockServer1);
        final KafkaBroker broker2 = new KafkaBroker(mockServer2);
        final KafkaBroker broker3 = new KafkaBroker(mockServer3);
        final KafkaBroker broker5 = new KafkaBroker(mockServer5);
        final List<KafkaBroker> brokerList = new ArrayList<>();
        brokerList.add(broker2);
        brokerList.add(broker1);
        brokerList.add(broker5);
        brokerList.add(broker3);

        // Create KafkaBrokers instance.
        final KafkaBrokers kafkaBrokers = new KafkaBrokers(brokerList);

        // Validate
        Assertions.assertEquals(4, kafkaBrokers.size());
    }
}