#!/bin/bash

echo "Building LMS for macOS..."
mvn clean package

echo "Packaging DMG with jpackage..."
jpackage --type dmg \
  --name "LMS" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer" \
  --input "target" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.Launcher" \
  --mac-package-name "LMS"

echo "Done! Installer is in target/installer/"
