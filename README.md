# cf-logmon

This application performs a blacbox test for measuring message reliability
when running the command `cf logs`. This is accomplished by writing groups of
logs, measuring the time it took to produce the logs, and then counting the
logs received in the log stream. This is one way to measure message
reliability of the Loggregator system.  The results of this test are displayed
in a simple UI and available via JSON and the Firehose.


## Setup

To get started you'll need to create a user that you would like to use within
the app (we recommend creating a specific user for performing the test rather
than using "real" credentials).  You will also need to install the jdk if you
are not setup for Java development.

1. Create a space auditor user.
   The application needs this user to read logs.
   Because its credentials will be stored in an environment variable in CF,
   **these credentials should not belong to any real user.**
   An example set of commands to create a user:
   ```bash
   ORG=my-org
   SPACE=my-space
   USERNAME=logmon-user@mycompany.com
   PASSWORD=fancy-password
   cf create-user <username> <password>
   cf set-space-role <username> <org> <space> SpaceAuditor
   ```

1.  ```bash
    git clone https://github.com/cloudfoundry-incubator/cf-logmon
    cd cf-logmon
    bin/deploy.rb
    ```

Then visit `https://cf-logmon.$CF_DOMAIN`.
You should start seeing data within 5 minutes.

We've found that statistics become more valuable after 24 hours (or after a
typical business day).

## Configuration

The following environment variables are used to configure test output and
rates:

* `LOGMON_PRODUCTION_LOG_CYCLES` - The number of logs to emit during each test
* `LOGMON_PRODUCTION_LOG_DURATION_MILLIS` - The amount of time in milliseconds
  during which the logs will be emitted.

It is also possible to configure various wait times:

* `LOGMON_TIME_BETWEEN_TESTS_MILLIS` - The amount of time to wait between each
  "test"
* `LOGMON_CONSUMPTION_POST_PRODUCTION_WAIT_TIME_MILLIS` - The amount of time
  to wait after production completes for all created logs to drain.
* `LOGMON_PRODUCTION_INITIAL_DELAY_MILLIS` - The amount of time to allow a log
  consumption connection to start before producing logs.

**Important** Do not scale this application beyond a single instance. Nothing
is done to distinquish app instances when consuming logs.

## Web UI

This application includes a simple user interface for understanding your loss
rate over the last 24 hours. The chart shoes the specific performance over the
last 24 hours. The anamoly journal shows events when your log reliability
rates falls below 99% (warning) and 90% (alert). This is a general guide to
help operators better understand how to configure metrics.

## Firehose Metrics

This application works best whenbound to the
[metrics-forwarder](https://network.pivotal.io/products/p-metrics-forwarder)
service.  This allows the following metrics to be emitted by the application.

* `metrics_forwarder.gauge.logmon.logs_produced`
* `metrics_forwarder.gauge.logmon.logs_consumed`

The metrics are tagged with the application GUID of the app that is pushed.

## Background

Due to the challenges of distributed systems, and untracked srouces of loss in
the Loggregator system setting Service Level Objectives for message
reliability has been difficult using whitebox monitoring tools. The
Loggregator team developed series of blackbox tests to monitor and mesaure
message reliability. This was developed as a stand alone applications through
a collaboration with Pivotal Labs in Denver.
