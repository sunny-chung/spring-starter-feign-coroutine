#!/bin/bash
set -e

./gradlew clean assemble bootJar
docker compose up --build
