package com.salesforce.kafka.test.junit5;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of SharedZookeeperTestResource.
 *
 * This also serves as an example of how to use this library!
 */
class SharedZookeeperTestResourceTest {
    /**
     * We have a single embedded zookeeper server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ExtendWith annotation.
     * It's automatically stopped after all of the tests are completed via the @ExtendWith annotation.
     * This instance is passed to this class's constructor via the @ExtendWith annotation.
     */
    @RegisterExtension
    public static final SharedZookeeperTestResource sharedZookeeperTestResource = new SharedZookeeperTestResource();

    /**
     * Validates that we receive a sane looking ZK connection string.
     */
    @Test
    void testGetZookeeperConnectString() {
        final String actualConnectStr = sharedZookeeperTestResource.getZookeeperConnectString();

        // Validate
        assertNotNull(actualConnectStr, "Should have non-null connect string");
        assertTrue(actualConnectStr.startsWith("127.0.0.1:"), "Should start with 127.0.0.1");
    }

    /**
     * Validates that we receive a sane looking ZK connection string.
     */
    @Test
    void testZookeeperServer() {
        final TestingServer zkTestServer = sharedZookeeperTestResource.getZookeeperTestServer();

        // Validate
        assertNotNull(zkTestServer, "Should have non-null instance");
    }
}