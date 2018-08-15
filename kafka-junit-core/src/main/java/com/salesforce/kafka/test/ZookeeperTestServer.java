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
 * Wrapper around TestingServer zookeeper test server instance.
 */
public class ZookeeperTestServer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTestServer.class);

    /**
     * Internal Zookeeper test server instance.
     */
    private TestingServer zkServer = null;

    /**
     * Starts the internal Test zookeeper server instance.
     */
    public void start() {
        logger.info("Starting Zookeeper test server");
        try {
            if (zkServer == null) {
                // Define configuration
                final InstanceSpec zkInstanceSpec = new InstanceSpec(
                    Utils.createTempDirectory(),
                    -1,
                    -1,
                    -1,
                    false,
                    -1,
                    -1,
                    1000
                );

                // Create instance
                zkServer = new TestingServer(zkInstanceSpec, true);
            } else {
                // Instance already exists, so 'start' by calling restart on instance.
                zkServer.restart();
            }
        } catch (final Exception exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * Restarts the internal Test zookeeper server instance.
     */
    public void restart() {
        // If we have no instance yet
        if (zkServer == null) {
            // Call start instead and return.
            start();
            return;
        }

        // Otherwise call restart.
        try {
            zkServer.restart();
        } catch (final Exception exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * Stops the internal Test zookeeper server instance.
     */
    public void stop() {
        logger.info("Shutting down zookeeper test server");

        // If we don't have an instance
        if (zkServer != null) {
            try {
                // Call stop, this keeps the temporary data on disk around
                // and allows us to be able to restart it again later.
                zkServer.stop();
            } catch (final IOException exception) {
                throw new RuntimeException(exception.getMessage(), exception);
            }
        }
    }

    /**
     * Alias for stop().
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Returns connection string for zookeeper clients.
     * @return Connection string to connect to the Zookeeper instance.
     * @throws IllegalStateException if start() has not been called yet.
     */
    public String getConnectString() {
        if (zkServer == null) {
            throw new IllegalStateException("Cannot get connect string before service is started.");
        }
        return zkServer.getConnectString();
    }
}