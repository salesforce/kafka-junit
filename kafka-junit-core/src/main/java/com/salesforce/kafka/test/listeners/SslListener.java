package com.salesforce.kafka.test.listeners;

import java.util.Properties;

public class SslListener implements BrokerListener {

    private int advertisedPort = 0;
    private String trustStoreFile = "";
    private String trustStorePassword = "";
    private String keyStoreFile = "";
    private String keyStorePassword = "";
    private String keyPassword = "";
    private boolean useSslForInterBrokerCommunications = true;
    private String clientAuth = "requested";

    public SslListener withAutoAssignedPort() {
        advertisedPort = 0;
        return this;
    }

    public SslListener withAssignedPort(final int port) {
        advertisedPort = port;
        return this;
    }

    public SslListener withTrustStoreLocation(final String trustStoreLocation) {
        this.trustStoreFile = trustStoreLocation;
        return this;
    }

    public SslListener withKeyStoreLocation(final String keyStoreLocation) {
        this.keyStoreFile = keyStoreLocation;
        return this;
    }

    public SslListener withTrustStorePassword(final String password) {
        this.trustStorePassword = password;
        return this;
    }

    public SslListener withKeyStorePassword(final String password) {
        this.keyStorePassword = password;
        return this;
    }

    public SslListener withKeyPassword(final String password) {
        this.keyPassword = password;
        return this;
    }

    public SslListener useSslForInterBrokerProtocol() {
        this.useSslForInterBrokerCommunications = true;
        return this;
    }

    public SslListener useSslForInterBrokerProtocol(final boolean value) {
        this.useSslForInterBrokerCommunications = value;
        return this;
    }

    public SslListener requireClientAuth() {
        this.clientAuth = "required";
        return this;
    }

    public SslListener requestedClientAuth() {
        this.clientAuth = "requested";
        return this;
    }

    @Override
    public String getProtocol() {
        return "SSL";
    }

    @Override
    public int getAdvertisedPort() {
        return advertisedPort;
    }

    @Override
    public Properties getBrokerProperties() {
        final Properties properties = new Properties();
        properties.put("ssl.truststore.location", trustStoreFile);
        properties.put("ssl.truststore.password", trustStorePassword);
        properties.put("ssl.keystore.location", keyStoreFile);
        properties.put("ssl.keystore.password", keyStorePassword);

        if (keyPassword != null && !keyPassword.isEmpty()) {
            properties.put("ssl.key.password", keyPassword);
        }

        if (useSslForInterBrokerCommunications) {
            // Set brokers to communicate via SSL as well.
            properties.put("security.inter.broker.protocol", "SSL");
        }
        properties.put("ssl.client.auth", clientAuth);

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
