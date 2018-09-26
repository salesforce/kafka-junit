# Kafka-JUnit-Core

This library wraps Apache Kafka's [KafkaServerStartable](https://github.com/apache/kafka/blob/1.1/core/src/main/scala/kafka/server/KafkaServerStartable.scala#L32) class and allows you to embed a single or multi-broker Kafka cluster within your application.  It's original
purpose was to support the [Kafka-JUnit4](../kafka-junit4) and [Kafka-JUnit5](../kafka-junit5) use cases, but could be useful for other situations.    

The most obvious use case would be for starting an embedded Kafka cluster within your application.  In development environments
for applications that depend on a multi-broker Kafka cluster, you could potentially avoid the need for a developer to manage and configure the service.  It should be noted 
that other tools exist that may solve this problem better, such as Docker.

## Example - Starting a test cluster as a command line application.
 
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

### Example - Starting a test cluster when SpringBoot application starts

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
     * Creates default admin user if none exists.
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
