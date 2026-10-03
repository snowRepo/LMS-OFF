#!/bin/bash

echo "Building LMS fat JAR..."
mvn clean package

echo "Isolating input files..."
rm -rf dist/input dist/arm dist/intel
mkdir -p dist/input
cp target/library-management-system-1.0.0.jar dist/input/
cp -r target/lib dist/input/

MODULES="java.base,java.desktop,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml"

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
  --name "LMS-macOS-AppleSilicon" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer" \
  --input "dist/input" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.Launcher" \
  --runtime-image dist/arm/runtime \
  --mac-package-name "LMS"

echo "Downloading Intel JDK 17 for cross-compilation..."
curl -fsSL "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.12_7.tar.gz" -o /tmp/jdk-intel.tar.gz
mkdir -p /tmp/intel-jdk
tar -xzf /tmp/jdk-intel.tar.gz -C /tmp/intel-jdk --strip-components=1

echo "Building Intel runtime via jlink..."
arch -x86_64 /tmp/intel-jdk/Contents/Home/bin/jlink \
  --module-path /tmp/intel-jdk/Contents/Home/jmods \
  --add-modules "$MODULES" \
  --output dist/intel/runtime \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=1

echo "Packaging DMG for Intel (x86_64) via Rosetta 2..."
arch -x86_64 /tmp/intel-jdk/Contents/Home/bin/jpackage --type dmg \
  --name "LMS-macOS-Intel" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer" \
  --input "dist/input" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.Launcher" \
  --runtime-image dist/intel/runtime \
  --mac-package-name "LMS"

echo "Done! Installers are in target/installer/"
