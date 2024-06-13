@echo off

:: Thin wrapper around jeka.ps1 to be friendly called from command line

:: use script present in distrib
set script="%~dp0jeka.ps1"
powershell.exe -ExecutionPolicy ByPass -File "%script%" %*