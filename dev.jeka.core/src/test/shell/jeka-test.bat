@echo off
set script="%~dp0jeka.ps1"
echo Use ps script : %script%
IF NOT EXIST %script% (
  echo %script% not found
  set script="%cd%jeka.ps1
)
:: powershell.exe -ExecutionPolicy ByPass -File "%script%" %*