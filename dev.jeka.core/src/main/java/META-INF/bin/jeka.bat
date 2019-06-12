@echo off

@rem Change here the default JVM options
@rem SET JEKA_OPTS == ""

@rem set terminal encoding to utf-8
chcp 65001 > nul

if not "%JEKA_JDK%" == "" set "JAVA_HOME=%JEKA_JDK%"
if "%JAVA_HOME%" == "" set "JAVA_CMD=java"
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

if exist %cd%\jeka\boot set "LOCAL_BUILD_DIR=.\jeka\boot\*;"
set "COMMAND="%JAVA_CMD%" %JEKA_OPTS% -cp "%LOCAL_BUILD_DIR%%JEKA_HOME%\dev.jeka.jeka-core.jar" dev.jeka.core.tool.Main %*"
if not "%JEKA_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off)
%COMMAND%
