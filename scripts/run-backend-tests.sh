#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/Users/kanikakholia/Library/Java/JavaVirtualMachines/temurin-21.0.11/Contents/Home}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mysql -u root -e "DROP DATABASE IF EXISTS drippay_integration_test; CREATE DATABASE drippay_integration_test;" 2>/dev/null || {
  echo "WARN: Could not reset drippay_integration_test — ensure MySQL is running"
}
cd "$ROOT/backend"
mvn -B test
