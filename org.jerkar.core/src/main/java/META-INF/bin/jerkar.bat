@echo off

@rem Change here the default JVM options 
@rem SET JERKAR_OPTS == ""

@rem set terminal encoding to utf-8
chcp 65001 >null

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


