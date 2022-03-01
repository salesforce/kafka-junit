#!/bin/bash

## Adjust values to suite requirements.
export KAFKA_VERSION=2.8.1
export KAFKA_SCALA_VERSION=kafka_2.12
export EXCLUDE_KAFKA_TESTS=0_11_0_x

mvn clean test -B -DkafkaVersion=$KAFKA_VERSION -DkafkaScalaVersion=$KAFKA_SCALA_VERSION -Dtests.excluded=$EXCLUDE_KAFKA_TESTS -DskipCheckStyle=true -Djava.security.auth.login.config=${PWD}/kafka-junit-core/src/test/resources/jaas.conf
