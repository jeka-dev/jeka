@echo off

@rem Change here the default JVM options
@rem SET JERKAR_OPTS == ""

@rem set terminal encoding to utf-8
chcp 65001 > nul

if not "%JERKAR_JDK%" == "" set "JAVA_HOME=%JERKAR_JDK%"
if "%JAVA_HOME%" == "" set "JAVA_CMD=java"
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

if exist %cd%\jerkar\boot set "LOCAL_BUILD_DIR=jerkar\boot\*;"
set "COMMAND="%JAVA_CMD%" %JERKAR_OPTS% -cp "%JERKAR_HOME%\dev.jeka.jeka-core.jar" dev.jeka.core.tool.Main %*"
if not "%JERKAR_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off)
%COMMAND%
