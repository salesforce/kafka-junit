package com.salesforce.kafka.test.listeners;

import java.util.Properties;

public class SaslPlainListener implements BrokerListener {
    private int advertisedPort = 0;

    public SaslPlainListener withAutoAssignedPort() {
        advertisedPort = 0;
        return this;
    }

    public SaslPlainListener withAssignedPort(final int port) {
        advertisedPort = port;
        return this;
    }

    @Override
    public String getProtocol() {
        return "SASL_PLAIN";
    }

    @Override
    public int getAdvertisedPort() {
        return advertisedPort;
    }

    @Override
    public Properties getBrokerProperties() {
        final Properties properties = new Properties();
        // TODO
        return properties;
    }

    @Override
    public Properties getClientProperties() {
        return new Properties();
    }
}
