#!/bin/bash

echo "Building LMS for Linux..."
mvn clean package

echo "Packaging DEB with jpackage..."
jpackage --type deb \
  --name "lms" \
  --description "Library Management System" \
  --vendor "DevApps" \
  --app-version "1.0.0" \
  --icon "src/main/resources/icons/icon.png" \
  --dest "target/installer" \
  --input "target" \
  --main-jar "library-management-system-1.0.0.jar" \
  --main-class "com.lms.App" \
  --linux-menu-group "Office"

mv target/installer/*.deb target/installer/LMS-Linux.deb

echo "Done! Installer is in target/installer/"
