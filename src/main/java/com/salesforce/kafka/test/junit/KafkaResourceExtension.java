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

package com.salesforce.kafka.test.junit;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension to provide an internal test kafka server to be shared across test cases within the same test class.
 *
 * Annotate your test class with:
 *   &#064;ExtendWith(KafkaResourceExtension.class)
 *
 * Add a constructor parameter to your test class:
 *
 *   public YourTestClass(SharedKafkaTestResource sharedKafkaTestResource) {
 *     // Save reference to test resource.
 *     this.sharedKafkaTestResource = sharedKafkaTestResource;
 *   }
 *
 * Within your test case methods:
 *   this.sharedKafkaTestResource.getKafkaTestServer()...
 */
public class KafkaResourceExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaResourceExtension.class);

    /**
     * Shared Kafka Test Server Resource.
     */
    private SharedKafkaTestResource kafkaTestResource = new SharedKafkaTestResource();

    /**
     * Here we stand up an internal test kafka and zookeeper service.
     * Once for all tests that use this shared resource.
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        logger.info("Starting kafka test server");

        // Start kafka test server
        kafkaTestResource
            .getKafkaTestServer()
            .start();
    }

    /**
     * Here we shut down the internal test kafka and zookeeper services.
     */
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        logger.info("Shutting down kafka test server");

        // Close out kafka test server if needed
        try {
            kafkaTestResource
                .getKafkaTestServer()
                .shutdown();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        kafkaTestResource = null;
    }

    @Override
    public boolean supportsParameter(
        final ParameterContext parameterContext,
        final ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext
            .getParameter()
            .getType()
            .equals(SharedKafkaTestResource.class);
    }

    @Override
    public Object resolveParameter(
        final ParameterContext parameterContext,
        final ExtensionContext extensionContext) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext
            .getParameter()
            .getType();

        if (parameterType.equals(SharedKafkaTestResource.class)) {
            return kafkaTestResource;
        }
        return null;
    }
}
