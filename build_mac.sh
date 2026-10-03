#!/bin/bash

echo "Building LMS for macOS..."
mvn clean package

echo "Packaging DMG with jpackage..."
jpackage --type dmg \
  --name "LMS" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.1" \
  --icon "src/main/resources/icons/icon.icns" \
  --dest "target/installer" \
  --input "target" \
  --main-jar "library-management-system-1.0.1.jar" \
  --main-class "com.lms.Launcher" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics \
  --java-options "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base" \
  --mac-package-name "LMS"

echo "Done! Installer is in target/installer/"
