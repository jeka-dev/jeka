@echo off

@rem Change here the default JVM options
@rem SET JEKA_OPTS == ""

@rem set terminal encoding to utf-8
chcp 65001 > nul

if not "%JEKA_JDK%" == "" set "JAVA_HOME=%JEKA_JDK%"
if "%JAVA_HOME%" == "" set "JAVA_CMD=java"
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

set "COMMAND="%JAVA_CMD%" %JEKA_OPTS% -cp "%~dp0jeka\wrapper\*" dev.jeka.core.wrapper.Booter "%~dp0." %*"
if not "%JEKA_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off)
%COMMAND%
