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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper containing immutable list of Kafka Brokers indexable by their brokerId.
 */
public class KafkaBrokers implements Iterable<KafkaBroker> {
    /**
     * Immutable mapping of brokerId to KafkaBroker instance.
     */
    private Map<Integer, KafkaBroker> brokerMap;

    /**
     * Constructor.
     * @param brokers List of KafkaBrokers in a cluster.
     */
    public KafkaBrokers(final Collection<KafkaBroker> brokers) {
        // Build immutable map.
        this.brokerMap = Collections.unmodifiableMap(brokers
            .stream()
            .collect(Collectors.toMap(KafkaBroker::getBrokerId, Function.identity()))
        );
    }

    /**
     * Lookup and return Kafka Broker by its id.
     * @param brokerId id of the broker.
     * @return KafkaBroker.
     * @throws IllegalArgumentException if requested an invalid broker id.
     */
    public KafkaBroker getBrokerById(final int brokerId) throws IllegalArgumentException {
        if (!brokerMap.containsKey(brokerId)) {
            throw new IllegalArgumentException("No broker exists with id " + brokerId);
        }
        return brokerMap.get(brokerId);
    }

    /**
     * Returns an immutable list of KafkaBroker instances.
     * @return Immutable List of Brokers.
     */
    public List<KafkaBroker> asList() {
        return Collections.unmodifiableList(
            new ArrayList<>(brokerMap.values())
        );
    }

    /**
     * Returns a stream of brokers.
     * @return Stream of Brokers.
     */
    public Stream<KafkaBroker> stream() {
        return asList().stream();
    }

    /**
     * Returns how many brokers are represented.
     * @return Number of brokers.
     */
    public int size() {
        return brokerMap.size();
    }

    @Override
    public Iterator<KafkaBroker> iterator() {
        return asList().iterator();
    }

    @Override
    public void forEach(final Consumer<? super KafkaBroker> action) {
        asList().forEach(action);
    }

    @Override
    public Spliterator<KafkaBroker> spliterator() {
        return asList().spliterator();
    }

    @Override
    public String toString() {
        return "KafkaBrokers{"
            + asList()
            + '}';
    }
}