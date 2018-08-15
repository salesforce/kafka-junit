# Kafka-JUnit5

This library wraps Apache Kafka's [KafkaServerStartable](https://github.com/apache/kafka/blob/1.1/core/src/main/scala/kafka/server/KafkaServerStartable.scala#L32) class and allows you to easily create and run tests against
one or more "real" kafka brokers running within your tests. No longer do you need to setup and coordinate with an external kafka cluster for your tests! 

Kafka-JUnit5 is built on-top of **JUnit 5** as an Extension using the **@RegisterExtension** annotation.

Kafka-JUnit5 works with Kafka versions **0.11.0.x**, **1.0.x**, **1.1.x**, and **2.0.x**. The library requires your project to explicitly declare/include Kafka in your project's POM dependency list.

For usage with JUnit4 or more general project information please review top level [README](../README.md). 

## Using Kafka-JUnit with JUnit 5.

### Usage & Examples

Include this library in your project's POM with test scope.  **You'll also need to include the appropriate Kafka libraries you want to test against.**

```xml
<!-- Declare kafka-junit5 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit5</artifactId>
    <version>3.0.1</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 2.0.x
```xml
<!-- Declare kafka-junit5 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit5</artifactId>
    <version>3.0.1</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 2.0.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 1.1.x

```xml
<!-- Declare kafka-junit5 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit5</artifactId>
    <version>3.0.1</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 1.1.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>1.1.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>1.1.1</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 1.0.x

```xml
<!-- Declare kafka-junit5 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit5</artifactId>
    <version>3.0.1</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 1.0.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>1.0.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>1.0.2</version>
    <scope>test</scope>
</dependency>
```

#### POM for Kafka 0.11.0.x

```xml
<!-- Declare kafka-junit5 dependency -->
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit5</artifactId>
    <version>3.0.1</version>
    <scope>test</scope>
</dependency>

<!-- Include Kafka 0.11.x -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>0.11.0.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>0.11.0.3</version>
    <scope>test</scope>
</dependency>
```

#### KafkaTestServer

A great example of how to use this can be found within our tests!  Check out [SharedKafkaTestResourceTest.java](src/test/java/com/salesforce/kafka/test/junit5/SharedKafkaTestResourceTest.java).

Add the following to your JUnit test class and it will handle automatically starting and stopping a single embedded Kafka broker for you.

```java
    /**
     * We have a single embedded Kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @RegisterExtension annotation.
     * It's automatically stopped after all of the tests are completed via the @RegisterExtension annotation.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();
```

SharedKafkaTestResource exposes the ability to override properties set on the Kafka broker instances.

```java
    /**
     * This is an example of how to override configuration values for the test Kafka broker instance.
     * 
     * Here we disable topic auto-creation, and set the message max bytes option to 512KB.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        .withBrokerProperty("auto.create.topics.enable", "false")
        .withBrokerProperty("message.max.bytes", "512000");
```

By default SharedKafkaTestResource will start only a single broker within the cluster. The following example will start
a cluster with 4 Kafka brokers. The Kafka brokers will have id's that start and increase from 1.

```java
    /**
     * This is an example of how start a multi-broker Kafka cluster in your tests. 
     * 
     * Here we configure SharedKafkaTestResource to start 4 brokers in the cluster.  The ids of the brokers
     * in this cluster will be: 1,2,3, and 4.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        .withBrokers(4);
```

[SharedKafkaTestResource](src/main/java/com/salesforce/kafka/test/junit5/SharedKafkaTestResource.java) instance has two accessors that you can make use of in your tests to interact with the service.

```java
    /**
     * @return bootstrap.servers string to configure Kafka consumers or producers to access the Kafka cluster.
     */
    public String getKafkaConnectString();
    
    /**
     * @return Connection string to connect to the Zookeeper instance backing the Kafka cluster.
     */
    public String getZookeeperConnectString();
    
    /**
     * @return Immutable list of brokers within the Kafka cluster, indexed by their brokerIds.
     */
    public KafkaBrokerList getKafkaBrokers();

    /**
     * KafkaTestUtils is a collection of re-usable/common access patterns for interacting with the Kafka cluster.
     * @return Instance of KafkaTestUtils configured to operate on the Kafka cluster.
     */
    public KafkaTestUtils getKafkaTestUtils();
```

#### KafkaTestUtils

Often times you'll end up rebuilding the same patterns in your tests against Kafka, such as creating topics, producing or consuming records from the Kafka cluster.
We've tried to collect many of these common patterns within [KafkaTestUtils](../kafka-junit-core/src/main/java/com/salesforce/kafka/test/KafkaTestUtils.java).

Below is a **sample** of the functionality available to you:

```java
    /**
     * Creates a topic in Kafka. If the topic already exists this does nothing.
     * @param topicName the namespace name to create.
     * @param partitions the number of partitions to create.
     * @param replicationFactor the number of replicas for the topic.
     */
    public void createTopic(final String topicName, final int partitions, final short replicationFactor);

    /**
     * Creates a Kafka producer configured to produce into internal Kafka cluster.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keySerializer Class of serializer to be used for keys.
     * @param valueSerializer Class of serializer to be used for values.
     * @param config Additional producer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaProducer<K, V> getKafkaProducer(
        final Class<? extends Serializer<K>> keySerializer,
        final Class<? extends Serializer<V>> valueSerializer,
        final Properties config);

    /**
     * Return Kafka Consumer configured to consume from internal Kafka cluster.
     * @param <K> Type of message key
     * @param <V> Type of message value
     * @param keyDeserializer Class of deserializer to be used for keys.
     * @param valueDeserializer Class of deserializer to be used for values.
     * @param config Additional consumer configuration options to be set.
     * @return KafkaProducer configured to produce into Test server.
     */
    public <K, V> KafkaConsumer<K, V> getKafkaConsumer(
        final Class<? extends Deserializer<K>> keyDeserializer,
        final Class<? extends Deserializer<V>> valueDeserializer,
        final Properties config);
    
    /**
     * Creates a Kafka AdminClient connected to our test server.
     * @return Kafka AdminClient instance.
     */
    public AdminClient getAdminClient();

    /**
     * Produce randomly generated records into a Kafka topic.
     * Use when you don't care about the contents of the records.
     *
     * @param numberOfRecords how many records to produce
     * @param topicName the topic to produce into.
     * @param partitionId the partition to produce into.
     * @return List of ProducedKafkaRecords.
     */
    public List<ProducedKafkaRecord<byte[], byte[]>> produceRecords(
        final int numberOfRecords,
        final String topicName,
        final int partitionId
    );

    /**
     * This will consume all records from all partitions on the given topic.
     * @param topic Topic to consume from.
     * @return List of ConsumerRecords consumed.
     */
    public List<ConsumerRecord<byte[], byte[]>> consumeAllRecordsFromTopic(final String topic);
```

#### Zookeeper Test Server

**Note** Since Kafka depends on Zookeeper, you get this for *free* if you use the SharedKafkaTestResource, you do not, and should not, use
 both of these together within the same Test class.

If you need to run tests against an **only** embedded Zookeeper server and not all of Kafka, we have you covered as well.  Add the following
 to your JUnit test class and it will handle automatically start and stopping the embedded Zookeeper instance for you.

```java
    /**
     * We have a single embedded zookeeper server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @RegisterExtension annotation.
     * It's automatically stopped after all of the tests are completed via the @RegisterExtension annotation.
     */
    @RegisterExtension
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