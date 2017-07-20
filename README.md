# cf-logmon

This application performs a blacbox test for measuring message reliability when 
running the command `cf logs`. This is accomplished by writing groups of logs, 
measuring the time it took to produce the logs, and then counting the logs received 
in the log stream. This is one way to measure message reliability of the Loggregator system.
The results of this test are displayed in a simple UI and available via JSON. 


## Setup
To get started you'll need to create a user that you would like to use within the app (we recommend creating a specific user for performing the test rather than using "real" credentials). 

1. Create a space auditor user.
   The application needs this user to read logs.
   Because its credentials will be stored in an environment variable in CF, **these credentials should not belong to any real user.**
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

We've found that statistics become more valuable after 24 hours (or after a typical business day).

### Configuration

To help CF operators better understand how their system performs under different types of load, 
    cf-logmon ships with a handful of "profiles".
Each of these profiles is intended to emulate applications with varying logging requirements.

In order to use a given profile, set the `LOGMON_PRODUCTION_APP_PROFILE` environment variable and then restage the application:

```bash
# Assuming an already deployed instance...
cf set-env cf-logmon LOGMON_PRODUCTION_APP_PROFILE noisy
cf restage cf-logmon
```

The following profiles are currently supported:

* noisy
* normal
* quiet

In addition to the production profile, it is also possible to configure various wait times:

* `LOGMON_TIME_BETWEEN_TESTS_MILLIS` -
  The amount of time to wait between each "test"
* `LOGMON_CONSUMPTION_POST_PRODUCTION_WAIT_TIME_MILLIS` -
  The amount of time to wait after production completes for all created logs to drain.
* `LOGMON_PRODUCTION_INITIAL_DELAY_MILLIS` -
  The amount of time to allow a log consumption connection to start before producing logs.

## Background

Historically, it has been difficult for Operators to understand when their deployed apps were experiencing log message loss.
The main goal of cf-logmon is to provide an interface for Operators/App Developers to monitor the log message reliability of their CF installation.

Through various tests, log message sampling has been found to be a good heuristic for determining when to scale parts of a CF installation.
cf-logmon, by treating CF (and by extension, Loggregator) as a black box, can provide a good semblance of the user experience for logs.

cf-logmon is not meant to provide a solution for, nor is it meant to help diagnose, message loss.
