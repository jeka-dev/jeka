@echo off
setlocal enableDelayedExpansion

@rem Change here the default JVM options
@rem SET JEKA_OPTS == ""

@rem set terminal encoding to utf-8
chcp 65001 > nul

set "JAVA_CMD=java"
if not "%JEKA_JDK%" == "" (
    set "JAVA_HOME=%JEKA_JDK%"
) else (
    call :findProp "." "jeka.java.version" version
    call :propJdkHome "." !version! result
    if not "!result!" == "" set JAVA_HOME=!result!
)
if not "%JAVA_HOME%" == "" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java"
    if not exist "!JAVA_CMD!.exe" (
        echo !JAVA_CMD! not found
        if not "%JEKA_JDK%" == "" (
            echo JEKA_JDK environment variable is pointing to invalid JDK directory %JEKA_JDK%
        ) else (
            echo JAVA_HOME environment variable is pointing to invalid JDK directory %JAVA_HOME%
            echo Please set JAVA_HOME or JEKA_JDK environment variable to point on a valid JDK directory.
        )
        exit /b 1
    )
)

if exist "%cd%\jeka\boot" set "LOCAL_BUILD_DIR=.\jeka\boot\*;"
if "%JEKA_HOME%" == "" set "JEKA_HOME=%~dp0"

rem Ensure that the Jeka jar is actually in JEKA_HOME
if not exist "%JEKA_HOME%\dev.jeka.jeka-core.jar" (
	echo Could not find "dev.jeka.jeka-core.jar" in "%JEKA_HOME%"
	echo Please ensure JEKA_HOME points to the correct directory
	echo or that the distrib.zip file has been extracted fully
	rem Pause before exiting so the user can read the message first
	pause
	exit /b 1
)
set "COMMAND="%JAVA_CMD%" %JEKA_OPTS% -cp "%LOCAL_BUILD_DIR%%JEKA_HOME%\dev.jeka.jeka-core.jar" dev.jeka.core.tool.Main %*"
if not "%JEKA_ECHO_CMD%" == "" (
	@echo on
	echo %COMMAND%
	@echo off)
%COMMAND%
exit /B %ERRORLEVEL%

:: read property in a property file. Call :prop propFile, propName, result
:prop
if exist "%~1" (
    for /F "eol=# delims== tokens=1,*" %%a in (%~1) do (
        if "%%a"=="%~2" (
            set %~3=%%b
        )
    )
)
exit /B %ERRORLEVEL%

:: find a property value in hierarchy local.properties files. Call :propJavaVersion curentDir, propName, result
:findProp
call :prop "%~1\jeka\local.properties" %~2 %~3
if "%~3" == "" (
    if exist %~1\..\jeka\ (
        call :findProp %~1\.. %~2 %~3
    )
)
exit /B %ERRORLEVEL%

:: call `propJdkVersion currentDir javaVersion result`
:propJdkHome
set "envVarName=JEKA_JDK_%~2"
if not "!%envVarName%!"=="" (
    set %~3=!%envVarName%!
) else (
    call :findProp %~1\.. "jeka.jdk.%~2" %~3
    if "!%~3!"=="" (
        if "%JEHA_USER_HOME%" == "" (
            set juh=%homedrive%%homepath%\.jeka
        ) else (
            set juh=%JEHA_USER_HOME%
        )
        call :prop "!juh!\global.properties" jeka.jdk.%~2 %~3
    )
)
exit /B %ERRORLEVEL%
