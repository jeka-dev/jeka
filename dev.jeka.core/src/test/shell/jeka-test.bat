@echo off
:Loop
  ::if "%1"=="" goto continue
  echo '%1'
  timeout /t 1
  shift
goto Loop
:continue