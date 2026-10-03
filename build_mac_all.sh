#!/bin/bash

echo "Building LMS fat JAR..."
mvn clean package

echo "Isolating input files..."
rm -rf dist/input dist/arm dist/intel
mkdir -p dist/input
cp target/library-management-system-1.0.1.jar dist/input/
cp -r target/lib dist/input/

MODULES="java.base,java.desktop,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics"

echo "Building Apple Silicon runtime via jlink..."
jlink \
  --add-modules "$MODULES" \
  --output dist/arm/runtime \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=1

echo "Packaging DMG for Apple Silicon (ARM64)..."
jpackage --type dmg \
  --name "LMS" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.1" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer/arm" \
  --input "dist/input" \
  --main-jar "library-management-system-1.0.1.jar" \
  --main-class "com.lms.Launcher" \
  --runtime-image dist/arm/runtime \
  --java-options "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base" \
  --mac-package-name "LMS"

mv target/installer/arm/LMS-1.0.1.dmg target/installer/LMS-macOS-AppleSilicon-1.0.1.dmg

echo "Downloading Intel JDK 17 for cross-compilation (Liberica)..."
curl -fsSL "https://download.bell-sw.com/java/17.0.12%2B10/bellsoft-jdk17.0.12+10-macos-amd64-full.tar.gz" -o /tmp/jdk-intel.tar.gz
mkdir -p /tmp/intel-jdk
tar -xzf /tmp/jdk-intel.tar.gz -C /tmp/intel-jdk --strip-components=1

echo "Building Intel runtime via jlink..."
arch -x86_64 /tmp/intel-jdk/bin/jlink \
  --module-path /tmp/intel-jdk/jmods \
  --add-modules "$MODULES" \
  --output dist/intel/runtime \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=1

echo "Packaging DMG for Intel (x86_64) via Rosetta 2..."
arch -x86_64 /tmp/intel-jdk/bin/jpackage --type dmg \
  --name "LMS" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.1" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer/intel" \
  --input "dist/input" \
  --main-jar "library-management-system-1.0.1.jar" \
  --main-class "com.lms.Launcher" \
  --runtime-image dist/intel/runtime \
  --java-options "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base" \
  --mac-package-name "LMS"

mv target/installer/intel/LMS-1.0.1.dmg target/installer/LMS-macOS-Intel-1.0.1.dmg

echo "Done! Installers are in target/installer/"
rm -rf target/installer/arm target/installer/intel
