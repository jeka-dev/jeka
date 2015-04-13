@echo off

@rem Change here the default JVM options 
if "%JAVA_OPTS%" == "" set "JAVA_OPTS=-Xmx512m -XX:MaxPermSize=512m" 

SET JERKAR_HOME=%~dp0
if "%JAVA_HOME%" == "" set "JAVA_CMD=java" 
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

set "COMMAND="%JAVA_CMD%" %JAVA_OPTS% -cp "%JERKAR_HOME%libs\ext\*;%JERKAR_HOME%org.jerkar.core.jar;%JERKAR_HOME%libs\required\*" org.jerkar.Main %*"
if not "%JERKAR_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off) 
%COMMAND%


