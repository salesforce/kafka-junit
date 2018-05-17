# Change Log
The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## 2.3.0 (5/17/2018)
- [Issue-12](https://github.com/salesforce/kafka-junit/issues/12) Added ability to pass broker properties to be used by test kafka service instance.
- Added helper method getAdminClient() on KafkaTestServer to get a configured AdminClient instance.
- Deprecated Kafka-JUnit5 @ExtendWith annotation implementations.  This has been replaced in favor of @RegisterExtension annotation.  Review [README.md](kafka-junit5/README.md) for more information on updated usage instructions.
- Deprecated KafkaTestServer constructor: `public KafkaTestServer(final String localHostname)`
  
  This constructor was replaced with the constructor `KafkaTestServer(final Properties overrideBrokerProperties)` where overrideBrokerProperties should contain the property `host.name` set to the hostname or IP address Kafka should use. 

## 2.2.0 (4/24/2018)
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

## 2.1.0 (4/24/2018)
 - Bungled release. Please use version 2.2.0.

## 2.0.0 (4/10/2018)
- Created new modules to support both JUnit4 and JUnit 5.

## 1.0.0 (09/11/2017)
- Initial release!
- Based off of Kafka Server and Kafka-Clients version 0.11.0.1
- Built for JUnit 4.x

