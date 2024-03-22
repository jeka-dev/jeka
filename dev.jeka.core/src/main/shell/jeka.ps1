#
# Script for launching JeKa tool in PowerShell.
#
# Authors: Jerome Angibaud
#
# Rules for selecting a JDK :
# - if JEKA_JDK_HOME env var is specified, select it
# - if a jeka.java.version property is specified
#     - if a jeka.jdk.[version] property is specified, select it.
#     - else, look in cache or download the proper JDK
# - else
#   - if JAVA_HOME env var is specified, select it
#   - else, look in cache and download default version (21)
#
# Rules for reading a property (said "my.prop") :
# - if a command line args contains "-Dmy.prop=xxx", returns 'xxx'
# - if an env var 'my.prop' exists, returns this value
# - if property is defined in $BASE_DIR/jeka.properties, returns this value
# - look recursively in $BASE_DIR/../jeka.properties. Stop at first folder ancestor not having a jeka.properties file
# - look in JEKA_USER_HOME/global.properties
#

#######################################
# Download specified zip/tgz file and unpack it to the specified dir
# Globals:
#   none
# Arguments:
#   $1 : url to download file
#   $2 : the target directory path to unzip/unpack content
#######################################
function DownloadAndUnpack {
  param([string]$url, [string]$dir)

  $temp_file = [System.IO.Path]::GetTempFileName();
  $webclient = New-Object System.Net.WebClient
  $webclient.DownloadFile($url, $temp_file)

  Expand-Archive -Force $temp_file $dir
  Remove-Item -Path $temp_file
}

#######################################
# Gets the Jeka directory for the user. This is where are located global.properties and cache dirs.
# Globals:
#   JEKA_USER_HOME (read)
# Arguments:
#   none
# Outputs:
#   Write location to stdout
#######################################
function Get-JekaUserHome {
  if ([string]::IsNullOrEmpty($env:JEKA_USER_HOME)) {
    return "$env:USERPROFILE\.jeka"
  }
  else {
    return $env:JEKA_USER_HOME
  }
}

#######################################
# Gets the effective cache dir for Jeka user
# Globals:
#   JEKA_CACHE_DIR (read)
#   JEKA_USER_HOME (read)
# Arguments:
#   none
# Outputs:
#   Write location to stdout
#######################################
function Get-CacheDir {
  if ([string]::IsNullOrEmpty($env:JEKA_CACHE_DIR)) {
    $jekaUserHome = Get-JekaUserHome
    return $jekaUserHome + "\cache"
  }
  else {
    return $env:JEKA_CACHE_DIR
  }
}

#######################################
# Gets the dir for caching projects cloned from git
# Globals:
#   JEKA_CACHE_DIR (read)
#   JEKA_USER_HOME (read)
# Arguments:
#   none
# Outputs:
#   Write location to stdout
#######################################
function Get-GitCacheDir {
  return "$(Get-CacheDir)\git"
}

#######################################
# Gets the sub-string part starting after '#' of a specified string. ('Hello#World' should returns 'World')
# Globals:
#   none
# Arguments:
#   $1 : the string to extract substring from
# Outputs:
#   Write extracted sub-string to stdout
#######################################
function Get-SubstringBeforeHash {
  param([string]$str)
  return $str.Split('#')[0]
}

#######################################
# Gets the sub-string part ending before '#' of a specified string. ('Hello#World' should returns 'Hello')
# Globals:
#   none
# Arguments:
#   $1 : the string to extract substring from
# Outputs:
#   Write extracted sub-string to stdout
#######################################
function Get-SubstringAfterHash {
  param([string]$str)
  return $str.Split('#')[1]
}

#######################################
# Gets the value of a property declared within a property file
# Globals:
#   none
# Arguments:
#   $1 : the path of the property file
#   $2 : the property name
#######################################
function Get-PropValueFromFile {
  param (
    [string]$filePath,
    [string]$propertyName
  )
  $data = Get-Content $filePath
  foreach ($line in $data) {
    if ($line -match "^$propertyName=") {
      $value = $line.Split('=')[1]
      return $value.Trim()
    }
  }
  return $null
}

#######################################
# Resolves and returns the value of a property by looking in command line args, env var and jeka.properties files
# Globals:
#   CMD_LINE_ARGS (read)
# Arguments:
#   $1 : the base directory from where looking for jeka.properties file
#   $2 : the property name
# Outputs:
#   Write env var name to stdout
#######################################
function Get-PropValueFromBaseDir {
  param([string]$baseDir, [Array]$cmdLineArgs, [string]$propName)

  $cmdArgsValue = Get-SystemPropFromArgs $cmdLineArgs $propName
  if ($null -ne $cmdArgsValue) {
    return $cmdArgsValue
  }
  $envValue = [Environment]::GetEnvironmentVariable($propName)
  if ($null -ne $envValue) {
    return $envValue
  }
  $jekaPropertyFilePath = Join-Path $baseDir 'jeka.properties'

  $value = Get-PropValueFromFile $jekaPropertyFilePath $propName
  if ($null -eq $value) {
    $parentDir = Join-Path $baseDir '..'
    $parentJekaPropsFile = Join-Path $parentDir 'jeka.properties'
    if (Test-Path $parentJekaPropsFile) {
      $value = Get-PropValueFromFile $parentJekaPropsFile $propName
    } else {
      $value = Get-PropValueFromFile $GLOBAL:GLOBAL_PROP_FILE $propName
    }
  }
  return $value
}

# return array
function ParseCommandLine([string]$cmdLine) {
  $pattern = '(""[^""]*""|[^ ]*)'
  $regex = New-Object Text.RegularExpressions.Regex $pattern
  $regex.Matches($cmdLine) | ForEach-Object { $_.Value.Trim('"') } | Where-Object { $_ -ne "" }
}

function Get-InterpolatedCmd {

  param(
    [string]$baseDir,
    [Array]$cmdLineArgs)

  $result = @()
  foreach ($arg in $cmdLineArgs) {
    if ($arg -like "::*") {
      $propKey= ( "jeka.cmd." + $arg.Substring(2) )
      $propValue= Get-PropValueFromBaseDir $baseDir $cmdLineArgs $propKey
      if ($null -ne $propValue) {
        $valueArray= Get-ParsedCommandLine $propValue
        $result += $valueArray
      } else {
        $result += $arg
      }
    } else {
      $result += $arg
    }
  }
  return $result
}

function Get-BaseDir {
  param([Array]$interpolatedArgs)

  $remoteArgs= @("-r", "--remote", "-ru", "-ur")
  $remoteIndex= Get-indexOfFirst $interpolatedArgs $remoteArgs
  if ($remoteIndex -eq -1) {
    return Get-Location
  }
  $remoteValue= $interpolatedArgs[($remoteIndex + 1)]
  if (IsGitRemote $remoteValue) {
    repoUrl= SubstringBeforeHash $remoteValue

  }
}

function IsGitRemote {
  param (
    [Parameter(Mandatory=$true)]
    [string]$remoteValue
  )
  $gitUrlRegex = '(https?://.+.git*)|^(ssh://.+.git*)|^(git://.+.git*)|^(git@[^:]+:.+.git*)$'
  if ($remoteValue -match $gitUrlRegex) {
    return $true;
  } else {
    return $false;
  }
}

function Get-FolderNameFromGitUrl {
  param (
    [Parameter(Mandatory=$true)]
    [string]$url
  )
  $protocols = @('https://', 'ssh://', 'git://', 'git@')
  $trimmedUrl = $url
  foreach ($protocol in $protocols) {
    $trimmedUrl = $trimmedUrl -replace [regex]::Escape($protocol), ''
  }
  # replace '/' by '_'
  $folderName = $trimmedUrl -replace '/', '_'
  return $folderName
}

function Get-JavaCmd {

}

class CmdLineArgs {
  [Array]$args

  CmdLineArgs([Array]$interpolatedArgs) {
    $this.args = $interpolatedArgs
  }

  [string] GetSystemProperty([string]$propName) {
    $prefix = "-D$propName="
    foreach ($arg in $this.args) {
      if ( $arg.StartsWith($prefix) ) {
        return $arg.Replace($prefix, "")
      }
    }
    return $null
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

  [CmdLineArgs] Interpolated([String]$globalPropFile) {
    $result = @()
    foreach ($arg in $cmdLineArgs) {
      if ($arg -like "::*") {
        $propKey= ( "jeka.cmd." + $arg.Substring(2) )
        $propValue= [Props]::GetValueFromFile($globalPropFile, $propKey)
        if ($null -ne $propValue) {
          $valueArray= ParseCommandLine $propValue
          $result += $valueArray
        } else {
          $result += $arg
        }
      } else {
        $result += $arg
      }
    }
    return [CmdLineArgs]::new($result)
  }


}

class Props {
  [CmdLineArgs]$cmdLineArgs
  [string]$baseDir

  Props([CmdLineArgs]$cmdLineArgs, [string]$baseDir) {
    $this.cmdLineArgs = $cmdLineArgs
    $this.baseDir = $baseDir
  }

  [string] GetValue([string]$propName) {
    $cmdArgsValue = $this.cmdLineArgs.GetSystemProperty($propName)
    if ($null -ne $cmdArgsValue) {
      return $cmdArgsValue
    }
    $envValue = [Environment]::GetEnvironmentVariable($propName)
    if ($null -ne $envValue) {
      return $envValue
    }
    $jekaPropertyFilePath = $this.baseDir + '\jeka.properties'

    $value = $this.GetValueFromFile($jekaPropertyFilePath, $propName)
    if ($null -eq $value) {
      $parentDir = $this.baseDir + '\..'
      $parentJekaPropsFile = $parentDir + '\jeka.properties'
      if (Test-Path $parentJekaPropsFile) {
        $value = $this.GetValueFromFile($parentJekaPropsFile, $propName)
      } else {
        $value = $this.GetValueFromFile($GLOBAL:GLOBAL_PROP_FILE, $propName)
      }
    }
    return $value
  }

  static [string] GetValueFromFile([string]$filePath, [string]$propertyName) {
    $data = [System.IO.File]::ReadAllLines($filePath)
    foreach ($line in $data) {
      if ($line -match "^$propertyName=") {
        $value = $line.Split('=')[1]
        return $value.Trim()
      }
    }
    return $null
  }

}

class JdkDownload {
  [string]$Version
  [string]$Distrib

  JdkDownload([string]$baseDir, [Array]$cmdLineArgs) {
    $this.Version = "21"
    $this.Distrib = "temurin"

  }

  Install() {
    $jdkurl="https://api.foojay.io/disco/v3.0/directuris?distro=$this.Distrib&javafx_bundled=false&libc_type=c_std_lib&archive_type=zip&operating_system=windows&package_type=jdk&version=$this.Version&architecture=x64&latest=available"
  }

}

function main([Array]$args) {

  $jekaUserHome = Get-JekaUserHome
  $globalPropFile = $jekaUserHome + "\global.properties"
  $rawCmdLineArgs = [CmdLineArgs]::new($args)
  $cmdLineArgs = $rawCmdLineArgs.Interpolated($globalPropFile)
  $baseDir = Get-BaseDir($interpolatedArgs)
  $props = [Props]::new($cmdLineArgs, $baseDir)






}



$ErrorActionPreference = "Stop"
