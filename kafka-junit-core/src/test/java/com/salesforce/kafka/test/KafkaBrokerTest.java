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