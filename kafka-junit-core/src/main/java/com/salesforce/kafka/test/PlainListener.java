package com.salesforce.kafka.test;

import java.util.Properties;

public class PlainListener implements RegisterListener {

    private String advertisedHost = "127.0.0.1";
    private int advertisedPort = 0;

    public PlainListener withAdvertisedHost(final String host) {
        this.advertisedHost = host;
        return this;
    }

    public PlainListener withAutoAssignedPort() {
        advertisedPort = 0;
        return this;
    }

    public PlainListener withAssignedPort(final int port) {
        advertisedPort = port;
        return this;
    }

    @Override
    public String getAdvertisedListener() {
        return "PLAINTEXT";
    }

    @Override
    public int getAdvertisedPort() {
        return advertisedPort;
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();
        return properties;
    }
}
