# Kafka-JUnit4

This library wraps Kafka Test Server and allows you to easily create and run tests against
a "real" kafka server running within your tests, no more needing to stand up an external kafka cluster!

Kafka-JUnit4 is built on-top of **JUnit 4** as a SharedResource using the **@ClassRule** annotation.

Kafka-JUnit4 works with Kafka versions **0.11.0.x**, **1.0.x**, and **1.1.x** and must be explicitly declared in your project's POM.

For usage with JUnit5 or more project information please review top level [README](../README.md).

## Using Kafka-JUnit with JUnit 4.

### Usage & Examples

Include this library in your project's POM with test scope.  **You'll also need to include the appropriate Kafka libraries you want to test against.**

```xml
<!-- Declare kafka-junit4 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit4</artifactId>
    <version>2.3.0</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 1.1.x

```xml
<!-- Declare kafka-junit4 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit4</artifactId>
    <version>2.3.0</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 1.1.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 1.0.x

```xml
<!-- Declare kafka-junit4 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit4</artifactId>
    <version>2.3.0</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 1.0.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>1.0.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>1.0.1</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 0.11.0.x

```xml
<!-- Declare kafka-junit4 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit4</artifactId>
    <version>2.3.0</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 0.11.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>0.11.0.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>0.11.0.2</version>
    <scope>test</scope>
</dependency>
```

#### KafkaTestServer

A great example of how to use this can be found within our tests!  Check out [SharedKafkaTestResourceTest.java](src/test/java/com/salesforce/kafka/test/junit4/SharedKafkaTestResourceTest.java).

Add the following to your JUnit test file and it will handle automatically starting and stopping the embedded Kafka 
instance for you.

```java
    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     */
    @ClassRule
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();
```

SharedKafkaTestResource exposes the ability to override properties set on the test kafka broker instance.

```java
    /**
     * This is an example of how to override configuration values for the test kafka broker instance.
     * 
     * Here we define the broker.id to be set to 1000, and disable topic auto-creation.
     */
    @ClassRule
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        .withBrokerProperty("broker.id", "1000")
        .withBrokerProperty("auto.create.topics.enable", "false");
```

[SharedKafkaTestResource](src/main/java/com/salesforce/kafka/test/junit4/SharedKafkaTestResource.java) instance has two accessors that you can make use of in your tests to interact with the service.

```java
    /**
     * @return Shared Kafka Test server instance.
     */
    public KafkaTestServer getKafkaTestServer();

    /**
     * @return Instance of KafkaTestUtils configured and ready to go.
     */
    public KafkaTestUtils getKafkaTestUtils();
```

#### KafkaTestUtils

Often times you'll end up rebuilding the same patterns around producing and consuming data from this internal
kafka server.  We've tried to collect some of these within [KafkaTestUtils](../kafka-junit-core/src/main/java/com/salesforce/kafka/test/KafkaTestUtils.java)!

For usage and examples, check out it's test at [KafkaTestUtilsTest](src/test/java/com/salesforce/kafka/test/junit4/KafkaTestUtilsTest.java).

#### Zookeeper Test Server

**Note** Since Kafka depends on Zookeeper, you get this for *free* if you use the SharedKafkaTestResource, you do not, and should not, use
 both of these together within the same Test class.

If you need to run tests against an **only** embedded Zookeeper server and not all of Kafka, we have you covered as well.  Add the following
 to your JUnit test class and it will handle automatically start and stopping the embedded Zookeeper instance for you.

```java
    /**
     * We have a single embedded zookeeper server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     */
    @ClassRule
    public static final SharedZookeeperTestResource sharedZookeeperTestResource = new SharedZookeeperTestResource();
```

[SharedZookeeperTestResource](src/main/java/com/salesforce/kafka/test/junit4/SharedZookeeperTestResource.java) has the following accessors that you can make use of in your tests to interact with the Zookeeper instance.

```java
    /**
     * @return Shared Zookeeper test server instance.
     */
    public TestingServer getZookeeperTestServer();

    /**
     * @return Connection string to connect to the Zookeeper instance.
     */
    public String getZookeeperConnectString();
```