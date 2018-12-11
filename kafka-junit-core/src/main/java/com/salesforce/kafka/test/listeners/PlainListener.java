package com.salesforce.kafka.test.listeners;

import java.util.Properties;

public class PlainListener implements BrokerListener {

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
    public String getProtocol() {
        return "PLAINTEXT";
    }

    @Override
    public int getAdvertisedPort() {
        return advertisedPort;
    }

    @Override
    public Properties getBrokerProperties() {
        return new Properties();
    }

    @Override
    public Properties getClientProperties() {
        return new Properties();
    }
}
