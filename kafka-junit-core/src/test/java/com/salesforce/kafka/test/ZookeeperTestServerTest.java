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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ZookeeperTestServerTest {

    /**
     * Test start() starts the service and gracefully handles being called multiple times.
     */
    @Test
    void testStart() throws Exception {
        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            // Start service.
            zookeeperTestServer.start();

            // Validate the zookeeper server appears to be functioning.
            testZookeeperConnection(zookeeperTestServer.getConnectString());

            // Call start again
            zookeeperTestServer.start();

            // Validate the zookeeper server appears to be functioning.
            testZookeeperConnection(zookeeperTestServer.getConnectString());
        }
    }

    /**
     * Test restart() starts the service gracefully if not started.
     */
    @Test
    void testRestartCold() throws Exception {
        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            // Start service by calling restart()
            zookeeperTestServer.restart();

            // Validate the zookeeper server appears to be functioning.
            testZookeeperConnection(zookeeperTestServer.getConnectString());
        }
    }

    /**
     * Test restart() restarts the service if already started.
     */
    @Test
    void testRestart() throws Exception {
        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            // Start service
            zookeeperTestServer.start();

            // Validate the zookeeper server appears to be functioning.
            testZookeeperConnection(zookeeperTestServer.getConnectString());

            // Call restart
            zookeeperTestServer.restart();

            // Validate the zookeeper server appears to be functioning.
            testZookeeperConnection(zookeeperTestServer.getConnectString());
        }
    }

    /**
     * Test restarting preserves data stored.
     */
    @Test
    void testRestartPreservesData() throws Exception {
        final String pathStr = "/preservedData" + System.currentTimeMillis();
        final String dataStr = "MySpecialData" + System.currentTimeMillis();

        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            // Start service
            zookeeperTestServer.start();

            ZooKeeper zkClient = null;
            try {
                // Create client
                zkClient = createZkClient(zookeeperTestServer.getConnectString());

                // Write data
                writeZkString(zkClient, pathStr, dataStr);
            } finally {
                // Close client.
                if (zkClient != null) {
                    zkClient.close();
                }
            }

            // Call restart on server.
            zookeeperTestServer.restart();

            // Attempt to read original data out.
            try {
                // Create client
                zkClient = createZkClient(zookeeperTestServer.getConnectString());

                // Write data
                final String result = readZkString(zkClient, pathStr);
                Assertions.assertEquals(dataStr, result);
            } finally {
                // Close client.
                if (zkClient != null) {
                    zkClient.close();
                }
            }
        }
    }

    /**
     * Test calling stop() then start() preserves data stored.
     */
    @Test
    void testStopAndStartPreservesData() throws Exception {
        final String pathStr = "/preservedStopAndStartData" + System.currentTimeMillis();
        final String dataStr = "MyOtherSpecialData" + System.currentTimeMillis();

        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            // Start service
            zookeeperTestServer.start();

            ZooKeeper zkClient = null;
            try {
                // Create client
                zkClient = createZkClient(zookeeperTestServer.getConnectString());

                // Write data
                writeZkString(zkClient, pathStr, dataStr);
            } finally {
                // Close client.
                if (zkClient != null) {
                    zkClient.close();
                }
            }

            // Call stop on server.
            zookeeperTestServer.stop();

            Thread.sleep(2000L);

            // Followed by start
            zookeeperTestServer.start();

            // Attempt to read original data out.
            try {
                // Create client
                zkClient = createZkClient(zookeeperTestServer.getConnectString());

                // Write data
                final String result = readZkString(zkClient, pathStr);
                Assertions.assertEquals(dataStr, result);
            } finally {
                // Close client.
                if (zkClient != null) {
                    zkClient.close();
                }
            }
        }
    }

    /**
     * Test calling getConnectString() before calling start on the service.
     * It's expected to throw an IllegalStateException.
     */
    @Test
    void testGetConnectStringBeforeStartingService() {
        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            Assertions.assertThrows(IllegalStateException.class, zookeeperTestServer::getConnectString);
        }
    }

    /**
     * Test calling getConnectString() after calling start on the service.
     */
    @Test
    void testGetConnectString() throws Exception {
        // Create instance.
        try (final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer()) {
            // Start service.
            zookeeperTestServer.start();

            // Ask for the connect String
            final String connectString = zookeeperTestServer.getConnectString();
            Assertions.assertNotNull(connectString);

            // Validate the zookeeper server appears to be functioning.
            testZookeeperConnection(connectString);
        }
    }

    /**
     * Attempts to validate a Zookeeper server by the following criteria.
     *      - connecting
     *      - create a new node
     *      - reading that node
     *      - closing connection
     *
     * @param zkConnectString Zookeeper host.
     */
    private void testZookeeperConnection(final String zkConnectString) throws Exception {
        final String pathStr = "/zkTest" + System.currentTimeMillis();
        final String dataStr = "zkData" + System.currentTimeMillis();

        // Attempt to connect using a zookeeper client.
        ZooKeeper zkClient = null;
        try {
            zkClient = createZkClient(zkConnectString);

            // Create a new node storing dataBytes.
            writeZkString(zkClient, pathStr, dataStr);

            // Read back out of Zookeeper
            final String resultStr = readZkString(zkClient, pathStr);

            // Validate we got what we expected.
            Assertions.assertEquals(dataStr, resultStr);
        } finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
    }

    /**
     * Helper method to create a zookeeper client.
     */
    private ZooKeeper createZkClient(final String zkConnectString) throws IOException, InterruptedException {
        // Blocks until connected, or 5 sec timeout expires.
        final CountDownLatch connectionLatch = new CountDownLatch(1);

        // Attempt to connect using a zookeeper client.
        final ZooKeeper zkClient = new ZooKeeper(zkConnectString, 1000, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectionLatch.countDown();
            }
        });

        // Wait for connection.
        connectionLatch.await(5, TimeUnit.SECONDS);

        return zkClient;
    }

    /**
     * Helper method to write data to zookeeper.
     */
    private void writeZkString(
        final ZooKeeper zkClient,
        final String path,
        final String data
    ) throws KeeperException, InterruptedException {
        // Create a new node storing dataBytes.
        zkClient.create(path, data.getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    /**
     * Helper method to read data from zookeeper.
     */
    private String readZkString(final ZooKeeper zkClient, final String path) throws KeeperException, InterruptedException {
        return new String(
            zkClient.getData(path, null, null),
            StandardCharsets.UTF_8
        );
    }
}