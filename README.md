# kafka-junit

[![Build Status](https://travis-ci.org/salesforce/kafka-junit.svg?branch=master)](https://travis-ci.org/salesforce/kafka-junit)

This library wraps Kafka Test Server and allows you to easily create and run tests against
a "real" kafka server running within your tests, no more needing to stand up an external kafka cluster!

Version **1.0.x** is built on-top of **JUnit 4** as a SharedResource using the **@ClassRule** annotation.

Version **2.0.x** is built on-top of **JUnit 5** as an Extension using the **@ExtendWith** annotation. 

## Using Kafka-JUnit with JUnit 5.

### Usage & Examples

Include this in your project's POM with scope test.

```
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit</artifactId>
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

[SharedKafkaTestResource](src/main/java/com/salesforce/kafka/test/junit/SharedKafkaTestResource.java) instance has two accessors that you can make use of in your tests to interact with the service.

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
kafka server.  We've tried to collect some of these within [KafkaTestUtils](src/main/java/com/salesforce/kafka/test/KafkaTestUtils.java)!

For usage and examples, check out it's test at [KafkaTestUtilsTest](src/test/java/com/salesforce/kafka/test/KafkaTestUtilsTest.java).

#### Zookeeper Test Server

**Note** Since Kafka depends on Zookeeper, you get this for *free* if you use the [KafkaResourceExtension](src/main/java/com/salesforce/kafka/test/junit/KafkaResourceExtension.java), you do not, and should not, use
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

[SharedZookeeperTestResource](src/main/java/com/salesforce/kafka/test/junit/SharedZookeeperTestResource.java) has the following accessors that you can make use of in your tests to interact with the Zookeeper instance.

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

## Using Kafka-JUnit with JUnit 4.

### Usage & Examples

Include this in your project with scope test.

```
<dependency>
    <groupId>com.salesforce.kafka.test</groupId>
    <artifactId>kafka-junit</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

#### KafkaTestServer

A great example of how to use this can be found within our tests!  Check out [KafkaTestServerTest.java](tree/RELEASE-1.0.x/src/test/java/com/salesforce/kafka/test/KafkaTestServerTest.java)

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

SharedKafkaTestResource has two accessors that you can make use of in your tests to interact with the service.

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
kafka server.  We've tried to collect some of these within [KafkaTestUtils](tree/RELEASE-1.0.x/src/main/java/com/salesforce/kafka/test/KafkaTestUtils.java)!

For usage and examples, check out it's test at [KafkaTestUtilsTest](tree/RELEASE-1.0.x/src/test/java/com/salesforce/kafka/test/KafkaTestUtilsTest.java).

#### Zookeeper Test Server

**Note** Since Kafka depends on Zookeeper, you get this for *free* if you use the SharedKafkaTestResource, you do not, and should not, use
 both of these together within the same Test class.

If you need to run tests against an **only** embedded Zookeeper server and not all of Kafka, we have you covered as well.  Add the following
 to your JUnit test file 
and it will handle automatically start and stopping the embedded Zookeeper instance for you.

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

SharedZookeeperTestResource has the following accessors that you can make use of in your tests to interact with the Zookeeper instance.

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

## Changelog

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

[View Changelog](CHANGELOG.md)

# Contributing

Found a bug? Think you've got an awesome feature you want to add? We welcome contributions!


## Submitting a Contribution

1. Search for an existing issue. If none exists, create a new issue so that other contributors can keep track of what you are trying to add/fix and offer suggestions (or let you know if there is already an effort in progress).  Be sure to clearly state the problem you are trying to solve and an explanation of why you want to use the strategy you're proposing to solve it.
1. Fork this repository on GitHub and create a branch for your feature.
1. Clone your fork and branch to your local machine.
1. Commit changes to your branch.
1. Push your work up to GitHub.
1. Submit a pull request so that we can review your changes.

*Make sure that you rebase your branch off of master before opening a new pull request. We might also ask you to rebase it if master changes after you open your pull request.*

## Acceptance Criteria

We love contributions, but it's important that your pull request adhere to some of the standards we maintain in this repository. 

- All tests must be passing!
- All code changes require tests!
- All code changes must be consistent with our checkstyle rules.
- New configuration options should have proper annotations and README updates generated.
- Great inline comments.

# Other Notes

## Checkstyle

We use checkstyle aggressively on source and tests, our config is located under the 'script' folder and can be imported into your IDE of choice.

## License

[View License](LICENSE.txt)
 