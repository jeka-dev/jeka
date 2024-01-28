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
  rm  $temp_file
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
  if ([string]::IsNullOrEmpty($env: JEKA_USER_HOME)) {
    return "$env:USERPROFILE\.jeka"
  }
  else {
    return $env: JEKA_USER_HOME
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
  if ([string]::IsNullOrEmpty($env: JEKA_CACHE_DIR)) {
    return "$env:JEKA_USER_HOME\cache"
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
function Substring-BeforeHash {
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
function Substring-AfterHash {
  param([string]$str)
  return $str.Split('#')[1]
}


#######################################
# Gets the value of a property, declared as '-Dprop.name=prop.value' in an array.
# Globals:
#   CMD_LINE_ARGS (read)
# Arguments:
#   $1 : the property name
# Outputs:
#   Write property value to stdout
#######################################
function Get-SystemPropFromArgs {
  param([string]$PropName, [Array]$CmdLineArgs)
  $prefix = "-D$PropName="
  foreach ($arg in $CmdLineArgs) {
    if ($arg.StartsWith($prefix)) {
      return ($arg -replace $prefix, "")
    }
  }
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
  param([string]$FilePath, [string]$PropName)
  if (-Not (Test-Path $FilePath)) {
    return
  }
  $content = Get-Content $FilePath
  foreach ($line in $content) {
    if ($line -match "^\\s*$PropName=") {
      return ($line -split "=", 2)[1]
    }
  }
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
  param([string]$BaseDir, [string]$PropName, [Array]$CmdLineArgs)

  $cmdArgsValue = Get-SystemPropFromCmdArgs -PropName $PropName -CmdLineArgs $CmdLineArgs
  if ($null -ne $cmdArgsValue) {
    return $cmdArgsValue
  }
  $envValue = [Environment]::GetEnvironmentVariable($PropName)
  if ($null -ne $envValue) {
    return $envValue
  }
  $jekaPropertyFilePath = Join-Path $BaseDir 'jeka.properties'
  $value = Get-PropValueFromFile -FilePath $jekaPropertyFilePath -PropName $PropName
  if ($null -eq $value) {
    $parentDir = Join-Path $BaseDir '..'
    $parentJekaPropsFile = Join-Path $parentDir 'jeka.properties'
    if (Test-Path $parentJekaPropsFile) {
      $value = Get-PropValueFromFile -FilePath $parentJekaPropsFile -PropName $PropName
    } else {
      $value = Get-PropValueFromFile -FilePath $GLOBAL:GLOBAL_PROP_FILE -PropName $PropName
    }
  }
  return $value
}