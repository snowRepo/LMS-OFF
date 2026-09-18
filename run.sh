#!/bin/bash
# LibraryMS — run script
# Usage: ./run.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Compiling..."
mvn compile -q

M2="$HOME/.m2/repository"
OFX="$M2/org/openjfx"
VER="17.0.11"

MODULE_PATH="\
$OFX/javafx-base/$VER/javafx-base-$VER-mac.jar:\
$OFX/javafx-base/$VER/javafx-base-$VER.jar:\
$OFX/javafx-controls/$VER/javafx-controls-$VER-mac.jar:\
$OFX/javafx-controls/$VER/javafx-controls-$VER.jar:\
$OFX/javafx-fxml/$VER/javafx-fxml-$VER-mac.jar:\
$OFX/javafx-fxml/$VER/javafx-fxml-$VER.jar:\
$OFX/javafx-graphics/$VER/javafx-graphics-$VER-mac.jar:\
$OFX/javafx-graphics/$VER/javafx-graphics-$VER.jar"

CLASSPATH="$SCRIPT_DIR/target/classes:\
$M2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar:\
$M2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar:\
$M2/org/postgresql/postgresql/42.7.1/postgresql-42.7.1.jar:\
$M2/org/checkerframework/checker-qual/3.41.0/checker-qual-3.41.0.jar:\
$M2/com/mysql/mysql-connector-j/8.2.0/mysql-connector-j-8.2.0.jar:\
$M2/com/google/protobuf/protobuf-java/3.21.9/protobuf-java-3.21.9.jar"

echo "Launching LibraryMS..."
java \
  --module-path "$MODULE_PATH" \
  --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp "$CLASSPATH" \
  com.lms.Launcher
