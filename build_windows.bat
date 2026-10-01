@echo off
echo Building LMS for Windows...
call mvn clean package

echo Packaging EXE with jpackage...
jpackage --type exe ^
  --name "LMS" ^
  --description "Library Management System" ^
  --vendor "DevApps" ^
  --app-version "1.0.0" ^
  --icon "src\main\resources\icons\icon.ico" ^
  --dest "target\installer" ^
  --input "target" ^
  --main-jar "library-management-system-1.0.0.jar" ^
  --main-class "com.lms.App" ^
  --win-menu ^
  --win-dir-chooser ^
  --win-shortcut

echo Done! Installer is in target\installer\
