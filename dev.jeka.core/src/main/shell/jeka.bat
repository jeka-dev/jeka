@echo off

:: use script present in distrib
set script="%~dp0jeka.ps1"

IF NOT EXIST %script% (
  set script="%cd%jeka.ps1"
)
powershell.exe -ExecutionPolicy ByPass -File "%script%" %*