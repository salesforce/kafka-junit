# kafka-junit

This library wraps Apache Kafka's [KafkaServerStartable](https://github.com/apache/kafka/blob/1.1/core/src/main/scala/kafka/server/KafkaServerStartable.scala#L32) class and allows you to easily create and run tests against
one or more "real" kafka brokers. No longer do you need to setup and coordinate with an external kafka cluster for your tests! The library transparently supports running a single or multi-broker cluster.  Running a multi-broker cluster allows you to validate how your software reacts under various error scenarios, such as when one or more brokers become unavailable.

## Features
- Support for JUnit 4 and JUnit 5.
- Support for all Kafka versions from 0.11.0.x through 2.8.x
- Support for running either single broker cluster, or multi-broker clusters.
- Support for PLAINTEXT, SASL_PLAINTEXT, SASL_SSL, and SSL listeners.

## Using Kafka-JUnit with JUnit 4.

Please review [Kafka-JUnit4 Readme](kafka-junit4/) for usage instructions with JUnit4.

## Using Kafka-JUnit with JUnit 5.

Please review [Kafka-JUnit5 Readme](kafka-junit5/) for usage instructions with JUnit5.

## Using Kafka-JUnit-Core.

For use cases where you want to embed Kafka broker/cluster within your existing software, you can make use of the core package directly.  Please review [Kafka-JUnit-Core Readme](kafka-junit-core/) for usage instructions.


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
- Great inline comments.

# Other Notes

## Checkstyle

We use checkstyle aggressively on source and tests, our config is located under the 'script' folder and can be imported into your IDE of choice.

## Releasing

Steps for proper release:
- Update release version: `mvn versions:set -DnewVersion=X.Y.Z`
- Validate and then commit version: `mvn versions:commit`
- Update CHANGELOG and README files.
- Merge to master.
- Deploy to Maven Central: `mvn clean deploy -P release-kafka-junit`
- Create release on Github project.

## Changelog

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

[View Changelog](CHANGELOG.md)

## License

BSD 3-Clause [View License](LICENSE.txt).
 