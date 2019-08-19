# cf-logmon

This application performs a blacbox test for measuring message reliability
when running the command `cf tail`. This is accomplished by writing groups of
logs and then counting the logs received in Log Cache. This is one way to
measure message reliability of Log Cache. The results of this test are displayed
in a simple UI and available via JSON.


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

* `SKIP_CERT_VERIFY` - Whether to skip ssl validation when connecting to CF
* `LOG_MESSAGES_PER_BATCH` - The number of logs to emit during each test
* `BATCH_EMIT_DURATION` - The amount of time during which the logs will be emitted
* `LOG_SIZE_BYTES` - The byte size of the log message to emit

It is also possible to configure various wait times:

* `RUN_INTERVAL` - The amount of time to wait between each "test"
* `LOG_TRANSIT_WAIT` - The amount of time to wait to collect results after logs have been emitted.

**Important** Do not scale this application beyond a single instance. Nothing
is done to distinquish app instances when consuming logs.

## API Endpoints

This API allows for understanding your loss rate over the last 24 hours. This is a general guide to help operators
better understand how to configure metrics.

* `/tests` returns JSON formatted reliability stats over the last 24 hours
* `/summary` returns today's reliability percentage
* `/anomalies` records when your log reliability rates falls below 99% (warning) and 90% (alert)
