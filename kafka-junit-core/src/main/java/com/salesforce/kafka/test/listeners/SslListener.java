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
 * Define and register an SSL listener on a Kafka broker.
 */
public class SslListener implements BrokerListener {

    private String trustStoreFile = "";
    private String trustStorePassword = "";
    private String keyStoreFile = "";
    private String keyStorePassword = "";
    private String keyPassword = "";
    private String clientAuth = "required";

    /**
     * Setter.
     * @param trustStoreLocation file path to TrustStore JKS file.
     * @return SslListener for method chaining.
     */
    public SslListener withTrustStoreLocation(final String trustStoreLocation) {
        this.trustStoreFile = trustStoreLocation;
        return this;
    }

    /**
     * Setter.
     * @param keyStoreLocation file path to KeyStore JKS file.
     * @return SslListener for method chaining.
     */
    public SslListener withKeyStoreLocation(final String keyStoreLocation) {
        this.keyStoreFile = keyStoreLocation;
        return this;
    }

    /**
     * Setter.
     * @param password Password for TrustStore.
     * @return SslListener for method chaining.
     */
    public SslListener withTrustStorePassword(final String password) {
        this.trustStorePassword = password;
        return this;
    }

    /**
     * Setter.
     * @param password Password for KeyStore.
     * @return SslListener for method chaining.
     */
    public SslListener withKeyStorePassword(final String password) {
        this.keyStorePassword = password;
        return this;
    }

    /**
     * Setter.
     * @param password Password for Key.
     * @return SslListener for method chaining.
     */
    public SslListener withKeyPassword(final String password) {
        this.keyPassword = password;
        return this;
    }

    /**
     * Set client auth as required.
     * @return SslListener for method chaining.
     */
    public SslListener withClientAuthRequired() {
        this.clientAuth = "required";
        return this;
    }

    /**
     * Set client auth as requested, but not required.
     * @return SslListener for method chaining.
     */
    public SslListener withClientAuthRequested() {
        this.clientAuth = "requested";
        return this;
    }

    @Override
    public String getProtocol() {
        return "SSL";
    }

    @Override
    public Properties getBrokerProperties() {
        final Properties properties = new Properties();
        properties.put("ssl.truststore.location", trustStoreFile);
        properties.put("ssl.truststore.password", trustStorePassword);
        properties.put("ssl.keystore.location", keyStoreFile);
        properties.put("ssl.keystore.password", keyStorePassword);
        properties.put("ssl.client.auth", clientAuth);

        if (keyPassword != null && !keyPassword.isEmpty()) {
            properties.put("ssl.key.password", keyPassword);
        }

        properties.put("security.inter.broker.protocol", "SSL");
        //properties.put("inter.broker.listener.name", "SSL");

        return properties;
    }

    @Override
    public Properties getClientProperties() {
        final Properties properties = new Properties();
        properties.put("security.protocol", "SSL");
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
