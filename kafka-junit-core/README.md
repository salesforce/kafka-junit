# Kafka-JUnit-Core

This library wraps Apache Kafka's [KafkaServerStartable](https://github.com/apache/kafka/blob/1.1/core/src/main/scala/kafka/server/KafkaServerStartable.scala#L32) class and allows you to embed a single or multi-broker Kafka cluster within your application.  It's original
purpose was to support [Kafka-JUnit4](../kafka-junit4) and [Kafka-JUnit5](../kafka-junit5), but it could be useful for other situations.    

The most obvious use case would be for starting an embedded Kafka cluster within your application.  In development environments
for applications that depend on a multi-broker Kafka cluster, you could potentially avoid the need for a developer to manage and configure the service.  It should be noted 
that other tools exist that may solve this problem better, such as Docker.

## Example 1: Starting a 3-node test cluster as a command line application.
 
```java
import com.salesforce.kafka.test.KafkaTestCluster;
import com.salesforce.kafka.test.KafkaTestUtils;

/**
 * Simplified example.
 */
public class TestClusterApplication {
    /**
     * Entry point for example.
     * @param args command line arguments.
     */
    public static void main(String[] args) throws Exception {
        // Right now we accept one parameter, the number of nodes in the cluster.
        final int clusterSize;
        if (args.length > 0) {
            // Pull in cluster size.
            clusterSize = Integer.parseInt(args[0]);
        } else {
            // If no argument, default to cluster size of 1.
            clusterSize = 1;
        }

        System.out.println("Starting up kafka cluster with " + clusterSize + " brokers");

        // Create a test cluster
        final KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(clusterSize);

        // Start the cluster.
        kafkaTestCluster.start();

        // Create a topic
        final String topicName = "TestTopicA";
        final KafkaTestUtils utils = new KafkaTestUtils(kafkaTestCluster);
        utils.createTopic(topicName, clusterSize, (short) clusterSize);

        // Publish some data into that topic
        for (int partition = 0; partition < clusterSize; partition++) {
            utils.produceRecords(1000, topicName, partition);
        }

        kafkaTestCluster
            .getKafkaBrokers()
            .stream()
            .forEach((broker) -> System.out.println(
                "Started broker with Id " + broker.getBrokerId() + " at " + broker.getConnectString()
            ));

        System.out.println("Cluster started at: " + kafkaTestCluster.getKafkaConnectString());

        try {
            // Wait forever.
            Thread
                .currentThread()
                .join();
        } finally {
            // On interrupt, shutdown the cluster.
            System.out.println("Shutting down cluster...");
            kafkaTestCluster.close();
        }
    }
}
```

### Example 2: Starting a 3-node test cluster when SpringBoot application starts.

```java
import com.salesforce.kafka.test.KafkaTestCluster;
import com.salesforce.kafka.test.KafkaTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Example for starting an embedded Kafka Cluster when SpringBoot application starts.
 * 
 * Typically you'd only fire this in development or test environments.
 */
@Component
public final class KafkaTestClusterRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTestClusterRunner.class);
    private KafkaTestCluster kafkaTestCluster = null;

    /**
     * Run on startup.
     */
    @Override
    public void run(final ApplicationArguments args) throws Exception {
        startKafkaService(3);
    }

    /**
     * Starts multi-broker kafka cluster.
     */
    private void startKafkaService(final int clusterSize) {
        kafkaTestCluster = new KafkaTestCluster(clusterSize);
        
        // Start the cluster.
        kafkaTestCluster.start();
        
        // Create default data
        createDefaultTopic("TestTopicA", clusterSize, (short) clusterSize);
        createDefaultTopic("TestTopicB", clusterSize, (short) clusterSize);
        
        // Log details about the cluster
        logger.info("Cluster started at: {}", kafkaTestCluster.getKafkaConnectString());
    }

    /**
     * Create topics with test data.
     */
    private void createDefaultTopic(final String topicName, final int partitions, final short replicaFactor) {
        // Create a topic
        final KafkaTestUtils utils = new KafkaTestUtils(kafkaTestCluster);
        utils.createTopic(
            topicName, 
            kafkaTestCluster, 
            (short) clusterSize
        );

        // Publish some data into that topic
        for (int partition = 0; partition < clusterSize; partition++) {
            utils.produceRecords(1000, topicName, partition);
        }
    }
    
    /** 
     * @return Kafka Test Cluster. 
     */
    public KafkaTestCluster getKafkaTestCluster() {
        return this.kafkaTestCluster;
    }
}
```

### Example starting a cluster with SSL support

```java
    /**
     * Starts multi-broker kafka cluster with SSL support.
     */
    private void startKafkaService(final int clusterSize) {
        // Create SSL listener
        final BrokerListener listener = new SslListener()
            .withClientAuthRequired()
            .withKeyStoreLocation("/path/to/your/kafka.keystore.jks")
            .withKeyStorePassword("YourKeyStorePassword")
            .withTrustStoreLocation("/path/to/your/kafka.truststore.jks")
            .withTrustStorePassword("YourTrustStorePassword")
            .withKeyPassword("YourKeyPassword");
        
        // Define any other broker properties you may need.
        final Properties brokerProperties = new Properties();

        // Create cluster
        kafkaTestCluster = new KafkaTestCluster(
            clusterSize,
            brokerProperties,
            Collections.singletonList(listener)
        );
        
        // Start the cluster.
        kafkaTestCluster.start();
        
        // Create default data
        createDefaultTopic("TestTopicA", clusterSize, (short) clusterSize);
        createDefaultTopic("TestTopicB", clusterSize, (short) clusterSize);
        
        // Log details about the cluster
        logger.info("Cluster started at: {}", kafkaTestCluster.getKafkaConnectString());
    }
```

### Example starting a cluster with SASL_PLAIN support

**NOTE:** Kafka reads in the JAAS file as defined by an Environment variable at JVM start up.  This property
can not be set at run time.
 
In order to make use of this Listener, you **must** start the JVM with the following argument:

 `-Djava.security.auth.login.config=/path/to/your/jaas.conf`

```java
    /**
     * Starts multi-broker kafka cluster with SASL_PLAIN support.
     */
    private void startKafkaService(final int clusterSize) {
        // Create SSL_PLAIN listener
        final BrokerListener listener = new SaslPlainListener()
            // Define your username and password
            .withUsername("kafkaclient")
            .withPassword("client-secret");
        
        // Define any other broker properties you may need.
        final Properties brokerProperties = new Properties();

        // Create cluster
        kafkaTestCluster = new KafkaTestCluster(
            clusterSize,
            brokerProperties,
            Collections.singletonList(listener)
        );
        
        // Start the cluster.
        kafkaTestCluster.start();
        
        // Create default data
        createDefaultTopic("TestTopicA", clusterSize, (short) clusterSize);
        createDefaultTopic("TestTopicB", clusterSize, (short) clusterSize);
        
        // Log details about the cluster
        logger.info("Cluster started at: {}", kafkaTestCluster.getKafkaConnectString());
    }
```

### Example starting a cluster with SASL_SSL support

**NOTE:** Kafka reads in the JAAS file as defined by an Environment variable at JVM start up.  This property
can not be set at run time.
 
In order to make use of this Listener, you **must** start the JVM with the following argument:

 `-Djava.security.auth.login.config=/path/to/your/jaas.conf`

```java
    /**
     * Starts multi-broker kafka cluster with SASL_SSL support.
     */
    private void startKafkaService(final int clusterSize) {
        // Create SASL_SSL listener
        final BrokerListener listener = new SaslSslListener()
            .withKeyStoreLocation("/path/to/your/kafka.keystore.jks")
            .withKeyStorePassword("YourKeyStorePassword")
            .withTrustStoreLocation("/path/to/your/kafka.truststore.jks")
            .withTrustStorePassword("YourTrustStorePassword")
            .withKeyPassword("YourKeyPassword")
            // Define your username and password
            .withUsername("kafkaclient")
            .withPassword("client-secret");
        
        // Define any other broker properties you may need.
        final Properties brokerProperties = new Properties();

        // Create cluster
        kafkaTestCluster = new KafkaTestCluster(
            clusterSize,
            brokerProperties,
            Collections.singletonList(listener)
        );
        
        // Start the cluster.
        kafkaTestCluster.start();
        
        // Create default data
        createDefaultTopic("TestTopicA", clusterSize, (short) clusterSize);
        createDefaultTopic("TestTopicB", clusterSize, (short) clusterSize);
        
        // Log details about the cluster
        logger.info("Cluster started at: {}", kafkaTestCluster.getKafkaConnectString());
    }
```
