package com.salesforce.kafka.test;

import java.util.Properties;

public class SaslListener implements RegisterListener {
    private String advertisedHost = "127.0.0.1";
    private int advertisedPort = 0;

    public SaslListener withAdvertisedHost(final String host) {
        this.advertisedHost = host;
        return this;
    }

    public SaslListener withAutoAssignedPort() {
        advertisedPort = 0;
        return this;
    }

    public SaslListener withAssignedPort(final int port) {
        advertisedPort = port;
        return this;
    }

    @Override
    public String getAdvertisedListener() {
        return "SASL_PLAIN";
    }

    @Override
    public int getAdvertisedPort() {
        return advertisedPort;
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();
        // TODO
        return properties;
    }

    @Override
    public Properties getClientProperties() {
        return new Properties();
    }
}
