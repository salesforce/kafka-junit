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

package com.salesforce.kafka.test.listeners;

import java.util.Properties;

/**
 * Define and register a SASL_SSL listener on a Kafka broker.
 *
 * NOTE: Kafka reads in the JAAS file as defined by an Environment variable at JVM start up.  This property
 * can not be set at run time.
 *
 * In order to make use of this Listener, you **must** start the JVM with the following:
 *  -Djava.security.auth.login.config=/path/to/your/jaas.conf
 */
public class SaslSslListener implements BrokerListener {
    // SASL Settings.
    private String username = "";
    private String password = "";

    // SSL Settings.
    private String trustStoreFile = "";
    private String trustStorePassword = "";
    private String keyStoreFile = "";
    private String keyStorePassword = "";
    private String keyPassword = "";
    private String clientAuth = "requested";

    /**
     * Setter.
     * @param username SASL username to authenticate with.
     * @return SaslSslListener for method chaining.
     */
    public SaslSslListener withUsername(final String username) {
        this.username = username;
        return this;
    }

    /**
     * Setter.
     * @param password SASL password to authenticate with.
     * @return SaslSslListener for method chaining.
     */
    public SaslSslListener withPassword(final String password) {
        this.password = password;
        return this;
    }

    /**
     * Setter.
     * @param trustStoreLocation file path to TrustStore JKS file.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withTrustStoreLocation(final String trustStoreLocation) {
        this.trustStoreFile = trustStoreLocation;
        return this;
    }

    /**
     * Setter.
     * @param keyStoreLocation file path to KeyStore JKS file.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withKeyStoreLocation(final String keyStoreLocation) {
        this.keyStoreFile = keyStoreLocation;
        return this;
    }

    /**
     * Setter.
     * @param password Password for TrustStore.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withTrustStorePassword(final String password) {
        this.trustStorePassword = password;
        return this;
    }

    /**
     * Setter.
     * @param password Password for KeyStore.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withKeyStorePassword(final String password) {
        this.keyStorePassword = password;
        return this;
    }

    /**
     * Setter.
     * @param password Password for Key.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withKeyPassword(final String password) {
        this.keyPassword = password;
        return this;
    }

    /**
     * Set client auth as required.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withClientAuthRequired() {
        this.clientAuth = "required";
        return this;
    }

    /**
     * Set client auth as requested, but not required.
     * @return SslListener for method chaining.
     */
    public SaslSslListener withClientAuthRequested() {
        this.clientAuth = "requested";
        return this;
    }

    @Override
    public String getProtocol() {
        return "SASL_SSL";
    }

    @Override
    public Properties getBrokerProperties() {
        final Properties properties = new Properties();
        properties.put("sasl.enabled.mechanisms", "PLAIN");
        properties.put("sasl.mechanism.inter.broker.protocol","PLAIN");
        properties.put("inter.broker.listener.name", "SASL_SSL");

        properties.put("ssl.truststore.location", trustStoreFile);
        properties.put("ssl.truststore.password", trustStorePassword);
        properties.put("ssl.keystore.location", keyStoreFile);
        properties.put("ssl.keystore.password", keyStorePassword);
        properties.put("ssl.client.auth", clientAuth);

        if (keyPassword != null && !keyPassword.isEmpty()) {
            properties.put("ssl.key.password", keyPassword);
        }
        return properties;
    }

    @Override
    public Properties getClientProperties() {
        final Properties properties = new Properties();
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", "SASL_SSL");
        properties.put(
            "sasl.jaas.config",
            "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
            + "username=\"" + username + "\"\n"
            + "password=\"" + password + "\";"
        );

        properties.put("ssl.truststore.location", trustStoreFile);
        properties.put("ssl.truststore.password", trustStorePassword);
        properties.put("ssl.keystore.location", keyStoreFile);
        properties.put("ssl.keystore.password", keyStorePassword);

        if (keyPassword != null && !keyPassword.isEmpty()) {
            properties.put("ssl.key.password", keyPassword);
        }

        return properties;
    }
}