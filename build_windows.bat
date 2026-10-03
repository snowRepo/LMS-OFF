@echo off
echo Building LMS for Windows...
call mvn clean package

echo Isolating input files...
if exist dist\input rmdir /s /q dist\input
mkdir dist\input
copy target\library-management-system-1.0.1.jar dist\input\
xcopy target\lib dist\input\lib\ /E /H /C /I

echo Packaging EXE with jpackage...
jpackage --type exe ^
  --name "LMS" ^
  --description "Library Management System" ^
  --vendor "DevApps" ^
  --app-version "1.0.1" ^
  --icon "src\main\resources\icons\icon.ico" ^
  --dest "target\installer" ^
  --input "dist\input" ^
  --main-jar "library-management-system-1.0.1.jar" ^
  --main-class "com.lms.Launcher" ^
  --add-modules java.base,java.desktop,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics ^
  --java-options "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base" ^
  --win-menu ^
  --win-dir-chooser ^
  --win-shortcut

ren target\installer\LMS-1.0.1.exe LMS-Windows.exe

echo Done! Installer is in target\installer\
