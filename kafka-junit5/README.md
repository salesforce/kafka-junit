# kafka-junit

This library wraps Kafka Test Server and allows you to easily create and run tests against
a "real" kafka server running within your tests, no more needing to stand up an external kafka cluster!

Kafka-JUnit5 is built on-top of **JUnit 5** as an Extension using the **@ExtendWith** annotation.

For usage with JUnit4 or more project information please review top level [README](../README.md). 

## Using Kafka-JUnit with JUnit 5.

### Usage & Examples

Include this in your project's POM with scope test.

```
<dependency>
    <groupId>com.salesforce.kafka.com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit5</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

#### KafkaTestServer

A great example of how to use this can be found within our tests!  Check out [KafkaTestServerTest.java](src/test/java/com/salesforce/kafka/test/KafkaTestServerTest.java)

Annotate your JUnit test class with `@ExtendWith(KafkaResourceExtension.class)` and add the appropriate constructor.  The JUnit5 extension will handle automatically starting and stopping the embedded Kafka 
instance for you.  

```java
@ExtendWith(KafkaResourceExtension.class)
public class MyTestClass {
    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ExtendWith annotation.
     * It's automatically stopped after all of the tests are completed via the @ExtendWith annotation.
     * This instance is passed to this class's constructor via the @ExtendWith annotation.
     */
    private final SharedKafkaTestResource sharedKafkaTestResource;

    /**
     * Constructor where KafkaResourceExtension provides the sharedKafkaTestResource object.
     * @param sharedKafkaTestResource Provided by KafkaResourceExtension.
     */
    public MyTestClass(final SharedKafkaTestResource sharedKafkaTestResource) {
        this.sharedKafkaTestResource = sharedKafkaTestResource;
    }
```

[SharedKafkaTestResource](kafka-junit5/src/main/java/test/junit/SharedKafkaTestResource.java) instance has two accessors that you can make use of in your tests to interact with the service.

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
kafka server.  We've tried to collect some of these within [KafkaTestUtils](kafka-junit5/src/main/java/test/KafkaTestUtils.java)!

For usage and examples, check out it's test at [KafkaTestUtilsTest](src/test/java/com/salesforce/kafka/test/KafkaTestUtilsTest.java).

#### Zookeeper Test Server

**Note** Since Kafka depends on Zookeeper, you get this for *free* if you use the [KafkaResourceExtension](kafka-junit5/src/main/java/test/junit/KafkaResourceExtension.java), you do not, and should not, use
both of these together within the same Test class.

If you need to run tests against an **only** embedded Zookeeper server and not all of Kafka, we have you covered as well.  Add the following annotation to your JUnit test class
 and it will handle automatically start and stopping the embedded Zookeeper instance for you.

```java
@ExtendWith(ZookeeperResourceExtension.class)
public class MyTestClass {
    /**
     * We have a single embedded zookeeper server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ExtendWith annotation.
     * It's automatically stopped after all of the tests are completed via the @ExtendWith annotation.
     * This instance is passed to this class's constructor via the @ExtendWith annotation.
     */
    private final SharedZookeeperTestResource sharedZookeeperTestResource;

    /**
     * Constructor where KafkaResourceExtension provides the sharedKafkaTestResource object.
     * @param sharedZookeeperTestResource Provided by ZookeeperResourceExtension.
     */
    public MyTestClass(final SharedZookeeperTestResource sharedZookeeperTestResource) {
        this.sharedZookeeperTestResource = sharedZookeeperTestResource;
    }
```

[SharedZookeeperTestResource](kafka-junit5/src/main/java/test/junit/SharedZookeeperTestResource.java) has the following accessors that you can make use of in your tests to interact with the Zookeeper instance.

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