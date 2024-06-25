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



function Get-LastVersion() {
  $metadataUrl = "https://repo1.maven.org/maven2/dev/jeka/jeka-core/maven-metadata.xml"
  $xmlContent = Invoke-WebRequest -Uri $metadataUrl
  $xml = [xml]$xmlContent
  $latestVersion = $xml.metadata.versioning.latest
  return $latestVersion
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
    if (Test-Path -Path $this.dir) {
      Remove-Item -Path $this.dir -Recurse -Force
    }
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

function Add-Path {
  param (
    [string]$DirectoryPath
  )

  # Determine the registry scope (user or machine)
  $registryScope = 'HKEY_CURRENT_USER'

  # Get the current PATH value from the registry
  $currentPath = Get-ItemProperty -Path "Registry::$registryScope\Environment" -Name PATH

  # Split the current PATH into an array of directories
  $existingPaths = $currentPath.Path.Split(';')

  # Check if the directory path is already in the PATH
  if ($existingPaths -notcontains $DirectoryPath) {
    # Add the new directory to the existing paths
    $newPath = $currentPath.Path + ";$DirectoryPath"

    # Update the PATH in the registry
    Set-ItemProperty -Path "Registry::$registryScope\Environment" -Name PATH -Value $newPath

    # Broadcast a WM_SETTINGCHANGE message to reload the environment
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")

    Write-Host "Added '$DirectoryPath' to the PATH."
  } else {
    Write-Host "'$DirectoryPath' is already in the PATH."
  }
}

function install() {
  param([string]$version)

  $mavenRepo = "https://repo1.maven.org/maven2"
  $url = $mavenRepo + "/dev/jeka/jeka-core/" + $version + "/jeka-core-" + $version + "-distrib.zip"
  $installDir = Get-JekaUserHome
  $installDir = "$installDir\bin"
  MessageInfo "Installing Jeka version $version in $installDir"
  $extractor = [ZipExtractor]::new($url, $installDir)
  $extractor.ExtractRootContent()
  Add-Path "$installDir"
}

function Main {
  param(
    [array]$arguments
  )

  # Get interpolated cmdLine, while ignoring Base dir
  $cmdLineArgs = [CmdLineArgs]::new($arguments)
  if ($cmdLineArgs.GetIndexOfFirstOf("install") -ne -1) {
    $version = Get-LastVersion
    install($version)
    if ($cmdLineArgs.GetIndexOfFirstOf("check") -ne -1) {
      MessageInfo "Checking install with 'jeka --version'. This requires JDK download."
      jeka --version
    }
    MessageInfo "JeKa $version is properly installed."
    MessageInfo "Later on, you can upgrade to a different JeKa version by running either 'jeka-update' or 'jeka-update <version>'."

  } else {
    $version
    if ($arguments.Count -eq 0) {
      $version = Get-LastVersion
    } else {
      $version = $arguments[0]
    }
    Write-Host "Updating Jeka to version $version ? [y/n]"
    $user_input = Read-Host
    if ($user_input -ne "y") {
      exit 1
    }
    install($version)
    MessageInfo "Jeka updated to version $version"
  }
}

$ErrorActionPreference = "Stop"
Main -arguments $args