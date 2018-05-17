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

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Wrapper around TestingServer zookeeper test server instance.  This is here to 'DRY' out the code
 * between the JUnit4 and JUnit5 implementations.
 */
public class ZookeeperTestServer {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTestServer.class);

    /**
     * Our internal Zookeeper test server instance.
     */
    private TestingServer zookeeperTestServer = null;

    /**
     * Starts the internal Test zookeeper server instance.
     */
    public void start() {
        logger.info("Starting Zookeeper test server");
        if (zookeeperTestServer != null) {
            throw new IllegalStateException("Unknown State! Zookeeper test server already exists!");
        }

        // Setup zookeeper test server
        final InstanceSpec zkInstanceSpec = new InstanceSpec(null, -1, -1, -1, true, -1, -1, 1000);
        try {
            zookeeperTestServer = new TestingServer(zkInstanceSpec, true);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Stops the internal Test zookeeper server instance.
     */
    public void stop() {
        logger.info("Shutting down zookeeper test server");

        // If we don't have an instance
        if (zookeeperTestServer != null) {
            try {
                zookeeperTestServer.stop();
                zookeeperTestServer.close();

                // null out reference
                zookeeperTestServer = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return Underlying shared Zookeeper test server instance.
     * @throws IllegalStateException if start() has not been called yet.
     */
    public TestingServer getZookeeperTestServer() {
        if (zookeeperTestServer == null) {
            throw new IllegalStateException("Unable to get test server instance before being created!");
        }
        return zookeeperTestServer;
    }

    /**
     * @return Connection string to connect to the Zookeeper instance.
     * @throws IllegalStateException if start() has not been called yet.
     */
    public String getZookeeperConnectString() {
        return getZookeeperTestServer().getConnectString();
    }
}
