#!/bin/bash

CF=$(which cf)
if [[ -z "$CF" ]]; then
  CF=/tmp/cf
fi

$(dirname $0)/../gradlew assemble -Dorg.gradle.project.version=1.0.$PATCH_NUM
$CF login -a "$CF_API" -u "$CF_USER" -p "$CF_PASSWORD" -s "$CF_SPACE"
$CF push "$CF_APPNAME" -p $(dirname $0)/../build/libs/rivendell-1.0.$PATCH_NUM.jar -b https://github.com/cloudfoundry/java-buildpack
