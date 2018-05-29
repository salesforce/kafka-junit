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

import com.salesforce.kafka.test.AbstractKafkaTestResource;
import com.salesforce.kafka.test.KafkaTestCluster;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Creates and stands up an internal test kafka server to be shared across test cases within the same test class.
 *
 * Example within your Test class.
 *
 *   &#064;ClassRule
 *   public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();
 *
 * Within your test case method:
 *   sharedKafkaTestResource.getKafkaTestServer()...
 */
public class SharedKafkaTestResource extends AbstractKafkaTestResource<SharedKafkaTestResource> implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(SharedKafkaTestResource.class);

    /**
     * Default constructor.
     */
    public SharedKafkaTestResource() {
        super();
    }

    /**
     * Constructor allowing passing additional broker properties.
     * @param brokerProperties properties for Kafka broker.
     */
    public SharedKafkaTestResource(final Properties brokerProperties) {
        super(brokerProperties);
    }

    /**
     * Here we stand up an internal test kafka and zookeeper service.
     * Once for all tests that use this shared resource.
     */
    private void before() throws Exception {
        logger.info("Starting kafka test server");
        if (getKafkaCluster() != null) {
            throw new IllegalStateException("Unknown State!  Kafka Test Server already exists!");
        }
        // Setup kafka test server
        setKafkaCluster(new KafkaTestCluster(getNumberOfBrokers(), getBrokerProperties()));
        getKafkaCluster().start();
    }

    /**
     * Here we shut down the internal test kafka and zookeeper services.
     */
    private void after() {
        logger.info("Shutting down kafka test server");

        // Close out kafka test server if needed
        if (getKafkaCluster() == null) {
            return;
        }
        try {
            getKafkaCluster().close();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        setKafkaCluster(null);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }
}
