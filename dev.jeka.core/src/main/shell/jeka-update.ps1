#Requires -Version 5

#
# Copyright 2014-2024  the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
#  limitations under the License.
#

#
# Author : Jérôme Angibaud
#
# Execute JeKa by Downloading requiered potentialy missing dependencies (JDK, JeKa version)
# The passed arguments are interpolated then passed to JeKA engine.
#

function MessageInfo {
  param([string]$msg)
  if ($global:QuietFlag -ne $true) {
    [Console]::Error.WriteLine($msg)
  }

}

function MessageVerbose {
  param([string]$msg)

  if (($VerbosePreference -eq "Continue") -and ($global:QuietFlag -ne $true)) {
    [Console]::Error.WriteLine($msg)
  }

}

function Exit-Error {
  param([string]$msg)

  Write-Host $msg -ForegroundColor Red
  exit 1
}

function Get-JekaUserHome {
  if ([string]::IsNullOrEmpty($env:JEKA_USER_HOME)) {
    return "$env:USERPROFILE\.jeka"
  }
  else {
    return $env:JEKA_USER_HOME
  }
}

function Get-CacheDir([string]$jekaUserHome) {
  if ([string]::IsNullOrEmpty($env:JEKA_CACHE_DIR)) {
    return $jekaUserHome + "\cache"
  }
  else {
    return $env:JEKA_CACHE_DIR
  }
}

class CmdLineArgs {
  [Array]$args

  CmdLineArgs([Array]$arguments) {
    $this.args = $arguments
  }


 [int] GetIndexOfFirstOf([Array]$candidates) {
    foreach ($arg in $candidates) {
      $index = $this.args.IndexOf($arg)
      if ($index -ne -1) {
        return $index
      }
    }
    return -1
  }

  [bool] IsUpdateFlagPresent() {
    $remoteArgs= @("-ru", "-ur", "-u", "--remote-update")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    return ($remoteIndex -ne -1)
  }

  [bool] IsQuietFlagPresent() {
    $remoteArgs= @("-q", "--quiet")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    return ($remoteIndex -ne -1)
  }

  [bool] IsVerboseFlagPresent() {
    $remoteArgs= @("-v", "--verbose", "--debug")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    return ($remoteIndex -ne -1)
  }

  [bool] IsProgramFlagPresent() {
    $remoteIndex= $this.GetProgramArgIndex()
    return ($remoteIndex -ne -1)
  }

}

class ZipExtractor {
  [string]$url
  [string]$dir

  ZipExtractor([string]$url, [string]$dir) {
    $this.url = $url
    $this.dir = $dir
  }

  ExtractRootContent() {
    $zipFile = $this.Download()
    $tempDir = [System.IO.Path]::GetTempFileName()
    Remove-Item -Path $tempDir
    Expand-Archive -Path $zipFile -DestinationPath $tempDir -Force
    $subDirs = Get-ChildItem -Path $tempDir -Directory
    $root = $tempDir + "\" + $subDirs[0]
    Move-Item -Path $root -Destination $this.dir -Force
    Remove-Item -Path $zipFile
    Remove-Item -Path $tempDir -Recurse
  }

  Extract() {
    $zipFile = $this.Download()
    Expand-Archive -Path $zipFile -DestinationPath $this.dir -Force
    Remove-Item -Path $zipFile
  }

  hidden [string] Download() {
    $downloadFile = [System.IO.Path]::GetTempFileName() + ".zip"
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($this.url, $downloadFile)
    $webClient.Dispose()
    return $downloadFile
  }
}

function Main {
  param(
    [array]$arguments
  )

  #$argLine = $arguments -join '|'
  #MessageInfo "Raw arguments |$argLine|"
  $jekaUserHome = Get-JekaUserHome
  $cacheDir = Get-CacheDir($jekaUserHome)
  $globalPropFile = $jekaUserHome + "\global.properties"

  # Get interpolated cmdLine, while ignoring Base dir
  $rawCmdLineArgs = [CmdLineArgs]::new($arguments)
  if ($rawCmdLineArgs.IsQuietFlagPresent()) {
    $global:QuietFlag = $true
  }
  $rawProps = [Props]::new($rawCmdLineArgs, $PWD.Path, $globalPropFile)
  $cmdLineArgs = $rawProps.InterpolatedCmdLine()

  # Resolve basedir and interpolate cmdLine according props declared in base dir
  $remoteArg = $cmdLineArgs.GetRemoteBaseDirArg()
  $updateArg = $cmdLineArgs.IsUpdateFlagPresent()
  $baseDirResolver = [BaseDirResolver]::new($remoteArg, $cacheDir, $updateArg)
  $baseDir = $baseDirResolver.GetPath()
  $props = [Props]::new($cmdLineArgs, $baseDir, $globalPropFile)
  $cmdLineArgs = $props.InterpolatedCmdLine()
  $joinedArgs = $cmdLineArgs.args -join " "
  if ($cmdLineArgs.IsVerboseFlagPresent()) {
    $VerbosePreference = 'Continue'
  }
  MessageVerbose "Interpolated cmd line : $joinedArgs"

  # Compute Java command
  $jdks = [Jdks]::new($props, $cacheDir)
  $javaCmd = $jdks.GetJavaCmd()

  # -p option present : try to execute program directly, bypassing jeka engine
  if ($cmdLineArgs.IsProgramFlagPresent()) {
    $progArgs = $cmdLineArgs.GetProgramArgs()  # arguments metionned after '-p'
    $progDir = $baseDir + "\jeka-output"
    $prog = [ProgramExecutor]::new($progDir, $progArgs)
    $progFile = $prog.FindProg()
    if ($progFile -ne '') {
      ExecProg -javaCmd $javaCmd -progFile $progFile -cmdLineArgs $progArgs
      exit $LASTEXITCODE
    }

    # No executable or Jar found : launch a build
    $buildCmd = $props.GetValue("jeka.program.build")
    MessageInfo "jeka.program.build=$buildCmd"
    if (!$buildCmd) {
      $srcDir = $baseDir + "\src"
      if ([System.IO.Directory]::Exists($srcDir)) {
        $buildCmd = "project: pack -Djeka.skip.tests=true --stderr"
      } else {
        $buildCmd = "base: pack -Djeka.skip.tests=true --stderr"
      }
    }
    $buildArgs = [Props]::ParseCommandLine($buildCmd)
    $leadingArgs = $cmdLineArgs.GetPriorProgramArgs()
    $effectiveArgs = $leadingArgs + $buildArgs
    MessageInfo "Building with command : $effectiveArgs"
    ExecJekaEngine -baseDir $baseDir -cacheDir $cacheDir -props $props -javaCmd $javaCmd -cmdLineArgs $effectiveArgs
    if ($LASTEXITCODE -ne 0) {
      Exit-Error "Build exited with error code $LASTEXITCODE. Cannot execute program"
    }
    $progFile = $prog.FindProg()
    if ($progFile -eq '') {
      Exit-Error "No program found to be executed in $progDir"
    }
    ExecProg -javaCmd $javaCmd -progFile $progFile -cmdLineArgs $progArgs
    exit $LASTEXITCODE

  # Execute Jeke engine
  } else {
    ExecJekaEngine -javaCmd $javaCmd -baseDir $baseDir -cacheDir $cacheDir -props $props -cmdLineArgs $cmdLineArgs.args
    exit $LASTEXITCODE
  }

}

$ErrorActionPreference = "Stop"
Main -arguments $args