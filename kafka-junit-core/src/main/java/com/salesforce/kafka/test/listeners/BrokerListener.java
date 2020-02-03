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

import java.util.Properties;

/**
 * This interface allows the caller to define and register a Listener on a Kafka Broker.
 * @see PlainListener for plaintext listeners (Default).
 * @see SslListener for SSL auth listeners.
 * @see SaslPlainListener for SASL auth listeners.
 * @see SaslSslListener for SASL+SSL auth listeners.
 */
public interface BrokerListener {

    /**
     * Returns the protocol name for the listener.
     * Examples being PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
     * @return Protocol name.
     */
    String getProtocol();

    /**
     * Define the properties required on the broker for this listener implementation.
     * @return Properties to be registered on broker.
     */
    Properties getBrokerProperties();

    /**
     * Define the properties required on the client to connect to the broker.
     * @return Properties to be registered on connecting client.
     */
    Properties getClientProperties();

    /**
     * The ports configured.
     * @return Configured ports.
     */
    int[] getPorts();

    /**
     * Internal method to get the next assigned port.  If called more times than configured ports,
     * this method will generate a random port to be used.
     *
     * @return next configured port to use.
     */
    int getNextPort();
}