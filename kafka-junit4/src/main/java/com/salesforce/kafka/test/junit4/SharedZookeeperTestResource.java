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

package com.salesforce.kafka.test.junit4;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Creates and stands up an internal test Zookeeper server to be shared across test cases within the same test class.
 *
 * Example within your Test class.
 *
 *   &#064;ClassRule
 *   public static final SharedZookeeperTestResource sharedZookeeperTestResource = new sharedZookeeperTestResource();
 *
 * Within your test case method:
 *   sharedZookeeperTestResource.getZookeeperTestServer()...
 */
public class SharedZookeeperTestResource extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(SharedZookeeperTestResource.class);

    /**
     * Our internal Zookeeper test server instance.
     */
    private TestingServer zookeeperTestServer = null;

    /**
     * Here we stand up an internal test zookeeper service.
     * Once for all tests that use this shared resource.
     */
    protected void before() throws Exception {
        logger.info("Starting Zookeeper test server");
        if (zookeeperTestServer != null) {
            throw new IllegalStateException("Unknown State! Zookeeper test server already exists!");
        }
        // Setup zookeeper test server
        final InstanceSpec zkInstanceSpec = new InstanceSpec(null, -1, -1, -1, true, -1, -1, 1000);
        zookeeperTestServer = new TestingServer(zkInstanceSpec, true);
    }

    /**
     * Here we shut down the internal test zookeeper service.
     */
    protected void after() {
        logger.info("Shutting down zookeeper test server");

        // Close out zookeeper test server if needed
        if (zookeeperTestServer == null) {
            return;
        }
        try {
            zookeeperTestServer.stop();
            zookeeperTestServer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        zookeeperTestServer = null;
    }

    /**
     * @return Shared Zookeeper test server instance.
     */
    public TestingServer getZookeeperTestServer() {
        return zookeeperTestServer;
    }

    /**
     * @return Connection string to connect to the Zookeeper instance.
     */
    public String getZookeeperConnectString() {
        return zookeeperTestServer.getConnectString();
    }
}