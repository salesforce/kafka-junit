# Change Log
The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## 3.2.6 (UNRELEASED)

- Add support for JDK21
- Add official support for Kafka versions 3.5.x through 3.8.x
- Kafka Scala version from kafka_2.12 to kafka_2.13
- JUnit5 from 5.9.2 to 5.11.0
- Apache Curator from 2.12.0 to 5.7.0

## 3.2.5 (02/21/2023)
- Add official support for Kafka versions 3.0.x through 3.4.x
- [gquintana](https://github.com/gquintana) added the following method to  `KafkaTestUtils` to allow for additional control over the records being produced.
  Thanks for the contribution!

```java
public List<ProducedKafkaRecord<byte[], byte[]>> produceRecords(final Collection<ProducerRecord<byte[], byte[]>> records)
```

### Internal dependency updates
- JUnit5 from 5.8.2 to 5.9.2
- Fix CI Build scripts

## 3.2.4 (03/01/2022)
### Bugfixes
- [ISSUE-49](https://github.com/salesforce/kafka-junit/issues/49) [PR-63](https://github.com/salesforce/kafka-junit/pull/63) Zookeeper AdminServer will no longer be started.  This allows for more easily running tests in parallel.  Thanks [@gquintana](https://github.com/gquintana) for the contribution!

### Internal dependency updates
- JUnit4 from 4.13 to 4.13.2
- JUnit5 from 5.7.2 to 5.8.2

## 3.2.3 (07/27/2021)
### Features
- Officially added support for Kafka versions 2.7.x, and 2.8.x

### Internal dependency updates
- JUnit5 from 5.6.2 to 5.7.2

## 3.2.2 (08/13/2020)
### Features
- [PR-46](https://github.com/salesforce/kafka-junit/pull/46) Officially added support for Kafka versions 2.5.x, 2.6.x.

### Internal dependency updates
- JUnit5 from 5.6.0 to 5.6.2

## 3.2.1 (02/03/2020)
[PR-42](https://github.com/salesforce/kafka-junit/pull/42)

### Features
- Officially added support for Kafka versions 2.1.x, 2.2.x, 2.3.x, 2.4.x.

### Bugfix
- Fixes bug in `ZookeeperTestServer.start()` where calling start on an already running instance caused the instance to be restarted instead of being a no-operation.
  This caused issues with Kafka version 2.1.x on startup and shutdown causing tests to hang.

### Internal dependency updates
- JUnit4 from 2.12 to 2.13
- JUnit5 from 5.3.2 to 5.6.0

## 3.2.0 (11/13/2019)
- [ISSUE-38](https://github.com/salesforce/kafka-junit/issues/38) Optionally allow for explicitly defining which ports kakfa brokers listen on.

## 3.1.2 (11/08/2019)
- [ISSUE-36](https://github.com/salesforce/kafka-junit/issues/36) Temporary directories should now be cleaned up properly on JVM shutdown.

## 3.1.1 (03/22/2019)
- Replace internal uses of Guava with JDK-comparable methods so that if a transitive dependency on Curator resolves to a more recent version that shades Guava this library will not break.

## 3.1.0 (12/13/2018)
- Officially support Kafka 2.0.x
- KafkaTestUtils.produceRecords() and its variants now set producer configuration "acks" to "all"
- Update Kafka-JUnit5 junit dependency from 5.1.1 to JUnit 5.3.2
- Add support for registering PLAINTEXT, SSL, SASL_PLAIN, and SASL_SSL listeners on internal kafka brokers.
- Kafka Connection Strings now include the protocol prefix.
- Default configured host changed from '127.0.0.1' to 'localhost'

## 3.0.1 (06/20/2018)
- [Issue-16](https://github.com/salesforce/kafka-junit/issues/16) Bugfix for re-using the same SharedKafkaTestResource instance multiple times, as might happen if you run a test multiple times.
- Update checkstyle plugin version to 3.0.0

## 3.0.0 (05/29/2018)
- Added ability to start 1 or more Kafka brokers configured within a functional cluster.  This allows you to now to test functionality that depends on more than one broker within your cluster, as well as scenarios around how your code handles broker availability.
- Expanded the methods available in the [KafkaTestUtils](kafka-junit-core/src/main/java/com/salesforce/kafka/test/KafkaTestUtils.java) class.

### Breaking Changes
- Several accessors were removed from SharedKafkaTestResource and should now be accessed
  via the KafkaTestUtils class.  In most cases simply changing from `sharedKafkaTestResource.getKafkaServer()....` to `sharedKafkaTestResource.getKafkaTestUtils()...` will be sufficient to migrate your code. 

## 2.3.0 (05/17/2018)
- [Issue-12](https://github.com/salesforce/kafka-junit/issues/12) Added ability to pass broker properties to be used by test kafka service instance.
- Added helper method getAdminClient() on KafkaTestServer to get a configured AdminClient instance.
- Deprecated Kafka-JUnit5 @ExtendWith annotation implementations.  This has been replaced in favor of @RegisterExtension annotation.  Review [README.md](kafka-junit5/README.md) for more information on updated usage instructions.
- Deprecated KafkaTestServer constructor: `public KafkaTestServer(final String localHostname)`
  
  This constructor was replaced with the constructor `KafkaTestServer(final Properties overrideBrokerProperties)` where overrideBrokerProperties should contain the property `host.name` set to the hostname or IP address Kafka should use. 

## 2.2.0 (04/24/2018)
- [Issue-5](https://github.com/salesforce/kafka-junit/issues/5) Updated to support Kafka versions 1.0.x and 1.1.x.  Thanks [kasuri](https://github.com/kasuri)!
- [Issue-4](https://github.com/salesforce/kafka-junit/issues/4) Fix server configuration to allow for transactional producers & consumers. 

### Breaking Change
This library now requires you to provide which version of Kafka you want to use.

See below for an example...for more details refer to README.

```xml
<!-- Declare kafka-junit4 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit4</artifactId>
    <version>2.2.0</version>
    <scope>test</scope>
</dependency>

<!-- Now required to include a kafka dependencies explicitly -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>0.11.0.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>0.11.0.1</version>
    <scope>test</scope>
</dependency>
```

## 2.1.0 (04/24/2018)
 - Bungled release. Please use version 2.2.0.

## 2.0.0 (04/10/2018)
- Created new modules to support both JUnit4 and JUnit 5.

## 1.0.0 (09/11/2017)
- Initial release!
- Based off of Kafka Server and Kafka-Clients version 0.11.0.1
- Built for JUnit 4.x

