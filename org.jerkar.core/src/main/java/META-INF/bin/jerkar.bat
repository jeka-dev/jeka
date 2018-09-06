@echo off

@rem Change here the default JVM options
@rem SET JERKAR_OPTS == ""

@rem set terminal encoding to utf-8
chcp 65001 > nul

if "%JAVA_HOME%" == "" set "JAVA_CMD=java"
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

if exist %cd%\build\boot set "LOCAL_BUILD_DIR=build\boot\*;"
set "COMMAND="%JAVA_CMD%" %JERKAR_OPTS% -cp "%JERKAR_HOME%\org.jerkar.core.jar" org.jerkar.tool.Main %*"
if not "%JERKAR_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off)
%COMMAND%
