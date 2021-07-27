/**
 * Copyright (c) 2017-2021, Salesforce.com, Inc.
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

import org.apache.curator.test.InstanceSpec;

/**
 * Shared Listener class.
 *
 * @param <Self> reference to parent class.
 */
public abstract class AbstractListener<Self> implements BrokerListener {
    /**
     * Defines which port(s) to listen on.
     */
    private int[] ports = {};
    private int portIndex = 0;

    /**
     * Optionally allow for explicitly defining which ports this listener will bind to.
     * Pass a unique port per broker running.
     *
     * If not explicitly called, random ports will be assigned to each listener and broker.
     *
     * @param ports the ports to bind to.
     * @return self for method chaining.
     */
    public Self onPorts(final int ... ports) {
        this.ports = ports;
        return (Self) this;
    }

    /**
     * The ports configured.
     * @return Configured ports.
     */
    public int[] getPorts() {
        return ports;
    }

    /**
     * Internal method to get the next assigned port.  If called more times than configured ports,
     * this method will generate a random port to be used.
     *
     * @return next configured port to use.
     */
    public int getNextPort() {
        if (ports == null || ports.length == 0 || portIndex >= ports.length) {
            // Return random Port
            return InstanceSpec.getRandomPort();
        }
        return ports[portIndex++];
    }
}
