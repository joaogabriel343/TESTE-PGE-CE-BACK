@REM Maven Wrapper Script for Windows
@REM https://maven.apache.org/wrapper/
@ECHO OFF
SET MAVEN_PROJECTBASEDIR=%~dp0
SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

IF EXIST "%WRAPPER_JAR%" GOTO runWithJava
FOR /F "tokens=1* delims==" %%A IN (%WRAPPER_PROPERTIES%) DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)
powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"

:runWithJava
"%JAVA_HOME%\bin\java.exe" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
