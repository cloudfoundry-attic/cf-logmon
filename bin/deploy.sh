#!/bin/bash

$(dirname $0)/../gradlew assemble -Dorg.gradle.project.version=1.0.$PATCH_NUM
/tmp/cf login -a "$CF_API" -u "$CF_USER" -p "$CF_PASSWORD" -s "$CF_SPACE"
/tmp/cf push "$CF_APPNAME" -p build/libs/rivendell-1.0.$PATCH_NUM.jar
