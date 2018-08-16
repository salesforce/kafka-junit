# Change Log
The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## UNRELEASED
- Officially support Kafka 2.0.0
- KafkaTestUtils.produceRecords() and its variants now set producer configuration "acks" to "all"
- Update Kafka-JUnit5 junit dependency from 5.1.1 to JUnit 5.2.0

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

