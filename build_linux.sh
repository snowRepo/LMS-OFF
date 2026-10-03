#!/bin/bash

echo "Building LMS for Linux..."
mvn clean package

echo "Isolating input files..."
rm -rf dist/input
mkdir -p dist/input
cp target/library-management-system-1.0.0.jar dist/input/
cp -r target/lib dist/input/

echo "Packaging DEB with jpackage..."
jpackage --type deb \
  --name "lms" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.png" \
  --dest "target/installer" \
  --input "dist/input" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.Launcher" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml \
  --linux-menu-group "Office"

mv target/installer/*.deb target/installer/LMS-Linux.deb

echo "Done! Installer is in target/installer/"
