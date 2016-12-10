@echo off

@rem Change here the default JVM options 
if "%JERKAR_OPTS%" == "" set "JERKAR_OPTS=-Xmx512m -XX:MaxPermSize=512m" 

SET JERKAR_HOME=%~dp0
if "%JAVA_HOME%" == "" set "JAVA_CMD=java" 
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

SET LOCAL_BUILD_DIR=
if exist %cd%\build\boot set "LOCAL_BUILD_DIR=build\boot\*;"
set "COMMAND="%JAVA_CMD%" %JERKAR_OPTS% -cp "%LOCAL_BUILD_DIR%%JERKAR_HOME%libs\ext\*;%JERKAR_HOME%org.jerkar.core-all.jar" org.jerkar.tool.Main %*"
if not "%JERKAR_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off) 
%COMMAND%


