package com.salesforce.kafka.test;

import java.util.Properties;

public class ConnectionProperties {
    private final String connectString;
    private final Properties clientProperties;

    public ConnectionProperties(final String connectString, final Properties clientProperties) {
        this.connectString = connectString;
        this.clientProperties = clientProperties;
    }

    public String getConnectionString() {
        return connectString;
    }

    public Properties getClientProperties() {
        return clientProperties;
    }
}
