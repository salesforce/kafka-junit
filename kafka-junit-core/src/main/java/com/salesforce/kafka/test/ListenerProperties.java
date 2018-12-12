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

package com.salesforce.kafka.test;

import java.util.Properties;

/**
 * Defines properties about a registered listener.
 */
public class ListenerProperties {
    private final String protocol;
    private final String connectString;
    private final Properties clientProperties;

    /**
     * Constructor.
     * @param protocol protocol of the listener.
     * @param connectString Connect string for listener.
     * @param clientProperties Any client properties required to connect.
     */
    public ListenerProperties(final String protocol, final String connectString, final Properties clientProperties) {
        this.protocol = protocol;
        this.connectString = connectString;
        this.clientProperties = clientProperties;
    }

    /**
     * Getter.
     * @return Name of protocol the listener is registered on.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Getter.
     * @return Connect string for talking to this listenre.
     */
    public String getConnectString() {
        return connectString;
    }

    /**
     * Getter.
     * @return Any Kafka client properties that need to be set to talk to this listener.
     */
    public Properties getClientProperties() {
        // Return a copy of the properties.
        final Properties copy = new Properties();
        copy.putAll(clientProperties);
        return copy;
    }
}
