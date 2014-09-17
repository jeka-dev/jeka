@echo off

@rem Change here the default JVM options 
if "%JAKE_JVM_OPTS%" == "" set "JAKE_JVM_OPTS=-Xmx512m -XX:MaxPermSize=512m" 

SET JAKE_HOME=%~dp0
if "%JAVA_HOME%" == "" set "JAVA_CMD=java" 
if not "%JAVA_HOME%" == "" set "JAVA_CMD=%JAVA_HOME%\bin\java"

set "COMMAND="%JAVA_CMD%" %JAKE_JVM_OPTS% -cp "%JAKE_HOME%libs\ext\*;%JAKE_HOME%jake.jar" org.jake.JakeLauncher %*"
if not "%JAKE_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off) 
%COMMAND%


