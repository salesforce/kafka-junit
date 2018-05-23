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

import com.salesforce.kafka.test.ZookeeperTestServer;
import org.apache.curator.test.TestingServer;
import org.junit.rules.ExternalResource;

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
    /**
     * Our internal Zookeeper test server instance.
     */
    private final ZookeeperTestServer zookeeperTestServer = new ZookeeperTestServer();

    /**
     * @return Connection string to connect to the Zookeeper instance.
     * @throws IllegalStateException if before() has not been called yet.
     */
    public String getZookeeperConnectString() throws IllegalStateException {
        return zookeeperTestServer.getZookeeperConnectString();
    }

    /**
     * Here we stand up an internal test zookeeper service.
     * once for all tests that use this shared resource.
     * @throws RuntimeException on startup errors.
     */
    protected void before() throws RuntimeException {
        zookeeperTestServer.start();
    }

    /**
     * Here we shut down the internal test zookeeper service.
     * @throws RuntimeException on shutdown errors.
     */
    protected void after() throws RuntimeException {
        zookeeperTestServer.stop();
    }
}