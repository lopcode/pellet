#!/usr/bin/env bash
set -eou pipefail

: "${VERSION?Need to set VERSION (eg 0.0.1)}"

PUBLISHING_USER=$(security find-generic-password -a "${USER}" -s PELLET_PUBLISHING_USER -w)
: "${PUBLISHING_USER?Need to set PUBLISHING_USER}"
export PUBLISHING_USER

PUBLISHING_PASSWORD=$(security find-generic-password -a "${USER}" -s PELLET_PUBLISHING_PASSWORD -w)
: "${PUBLISHING_PASSWORD?Need to set PELLET_PUBLISHING_PASSWORD}"
export PUBLISHING_PASSWORD

export SIGNING_KEY_ID="785B1DE054B17BDA"
export PUBLISHING_URL="https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"

./gradlew clean check
./gradlew bom:publishMavenBomPublicationToMavenRepository --stacktrace
./gradlew logging:publishMavenJavaPublicationToMavenRepository --stacktrace
./gradlew server:publishMavenJavaPublicationToMavenRepository --stacktrace