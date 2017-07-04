# cf-logmon

An application that provides blackbox monitoring for message reliability.

## Usage

In its simplest incarnation, cf-logmon operates without any configuration, simply clone and push. # CF mantra??

```bash
git clone https://github.com/cloudfoundry-incubator/cf-logmon
cd cf-logmon
cf push cf-logmon
```

Then visit `cf-logmon.cf-url.com`.
You should start seeing data within 5 minutes.

We've found that statistics become more valuable after 24 hours (or after a typical business day).

### Fiddling

To help CF operators better understand how their system performs under certain types of load, cf-logmon ships with a handful of "profiles".
Each of these profiles is intended to emulate applications with varying logging requirements.

In order to use a given profile, set the PERFORMANCE_PROFILE environment variable and then restage the application:

```bash
cf push cf-logmon
cf set-env cf-logmon PERFORMANCE_PROFILE noisy
cf restage cf-logmon
```

The following profiles are currently supported:

* noisy
* normal
* quiet

## Background

Historically, it has been difficult for Operators to understand when their deployed apps were experiencing log message loss.
The main goal of cf-logmon is to provide an interface for Operators/App Developers to monitor the log message reliability of their CF installation.

Through various tests, log message sampling has been found to be a good heuristic for determining when to scale parts of a CF installation.
cf-logmon, by treating CF (and by extension, Loggregator) as a black box, can provide a good semblance of the user experience for logs.

cf-logmon is not meant to provide a solution for, nor is it meant to help diagnose, message loss.
