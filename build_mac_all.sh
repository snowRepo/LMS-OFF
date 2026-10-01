#!/bin/bash

echo "Building LMS fat JAR..."
mvn clean package

echo "Packaging DMG for Apple Silicon (ARM64)..."
jpackage --type dmg \
  --name "LMS-macOS-AppleSilicon" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer" \
  --input "target" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.App" \
  --mac-package-name "LMS" \
  --resource-dir "src/main/resources/packaging/mac"

echo "Downloading Intel JDK 17 for cross-compilation..."
curl -fsSL "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.12_7.tar.gz" -o /tmp/jdk-intel.tar.gz
mkdir -p /tmp/intel-jdk
tar -xzf /tmp/jdk-intel.tar.gz -C /tmp/intel-jdk --strip-components=1

echo "Packaging DMG for Intel (x86_64) via Rosetta 2..."
arch -x86_64 /tmp/intel-jdk/Contents/Home/bin/jpackage --type dmg \
  --name "LMS-macOS-Intel" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer" \
  --input "target" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.App" \
  --mac-package-name "LMS" \
  --resource-dir "src/main/resources/packaging/mac"

echo "Done! Installers are in target/installer/"
