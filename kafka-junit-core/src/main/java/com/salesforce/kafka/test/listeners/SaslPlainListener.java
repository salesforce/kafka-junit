/**
 * Copyright (c) 2017-2020, Salesforce.com, Inc.
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

package com.salesforce.kafka.test.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Define and register a SASL_PLAIN listener on a Kafka broker.
 *
 * NOTE: Kafka reads in the JAAS file as defined by an Environment variable at JVM start up.  This property
 * can not be set at run time.
 *
 * In order to make use of this Listener, you **must** start the JVM with the following:
 *  -Djava.security.auth.login.config=/path/to/your/jaas.conf
 */
public class SaslPlainListener extends AbstractListener<SaslPlainListener> {
    private static final Logger logger = LoggerFactory.getLogger(SaslPlainListener.class);

    private String username = "";
    private String password = "";

    /**
     * Constructor.
     * Only purpose is to emit an ERROR log message if the System environment variable
     * java.security.auth.login.config has not be set.
     */
    public SaslPlainListener() {
        if (!JaasValidationTool.isJaasEnvironmentValueSet()) {
            logger.error("Missing required environment variable set: " + JaasValidationTool.JAAS_VARIABLE_NAME);
        }
    }

    /**
     * Setter.
     * @param username SASL username to authenticate with.
     * @return SaslPlainListener for method chaining.
     */
    public SaslPlainListener withUsername(final String username) {
        this.username = username;
        return this;
    }

    /**
     * Setter.
     * @param password SASL password to authenticate with.
     * @return SaslPlainListener for method chaining.
     */
    public SaslPlainListener withPassword(final String password) {
        this.password = password;
        return this;
    }

    @Override
    public String getProtocol() {
        return "SASL_PLAINTEXT";
    }

    @Override
    public Properties getBrokerProperties() {
        final Properties properties = new Properties();
        properties.put("sasl.enabled.mechanisms", "PLAIN");
        properties.put("sasl.mechanism.inter.broker.protocol","PLAIN");

        //properties.put("inter.broker.listener.name", "SASL_PLAINTEXT");
        properties.put("security.inter.broker.protocol", "SASL_PLAINTEXT");

        return properties;
    }

    @Override
    public Properties getClientProperties() {
        final Properties properties = new Properties();
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", "SASL_PLAINTEXT");
        properties.put(
            "sasl.jaas.config",
            "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
            + "username=\"" + username + "\"\n"
            + "password=\"" + password + "\";"
        );
        return properties;
    }
}
