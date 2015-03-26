@echo off

@rem Change here the default JVM options 
if "%JAVA_OPTS%" == "" set "JAVA_OPTS=-Xmx512m -XX:MaxPermSize=512m" 

SET JAKE_HOME=%~dp0
if "%JAVA_HOME%" == "" set "JAVA_CMD=java" 
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

set "COMMAND="%JAVA_CMD%" %JAVA_OPTS% -cp "%JAKE_HOME%libs\ext\*;%JAKE_HOME%org.jake.core.jar;%JAKE_HOME%libs\required\*" org.jake.Main %*"
if not "%JAKE_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off) 
%COMMAND%


