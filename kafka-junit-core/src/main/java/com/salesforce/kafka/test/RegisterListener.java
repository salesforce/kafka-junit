package com.salesforce.kafka.test;

import java.util.Properties;

public interface RegisterListener {
    String getAdvertisedListener();
    int getAdvertisedPort();
    Properties getProperties();
    Properties getClientProperties();
}
