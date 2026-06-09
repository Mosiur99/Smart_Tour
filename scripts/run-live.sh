#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-19-openjdk-amd64}"
export SPRING_PROFILES_ACTIVE=prod
export PORT="${PORT:-8080}"

echo "Building SmartTour..."
mvn -q package -DskipTests

echo "Starting on http://localhost:${PORT}"
java -jar target/smart-tour-planner-1.0.0.war
