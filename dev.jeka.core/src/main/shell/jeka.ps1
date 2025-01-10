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

class BaseDirResolver {
  [string]$url
  [string]$cacheDir
  [bool]$updateFlag

  BaseDirResolver([string]$url, [string]$cacheDir, [bool]$updateFlag) {
    $this.url = $url
    $this.cacheDir = $cacheDir
    $this.updateFlag = $updateFlag
  }

  [string] GetPath() {
    if ($this.url -eq '') {
      return $PWD.Path
    }
    if ($this.IsGitRemote() -eq $false) {
      return $this.url
    }
    $path = $this.cacheDir + "\git\" + $this.FolderName()
    if ([System.IO.Directory]::Exists($path)) {
      if ($this.updateFlag) {
        Remove-Item -Path $path -Recurse -Force -ErrorAction SilentlyContinue
        $this.GitClone($path)
      }
    } else {
      $this.GitClone($path)
    }
    return $path
  }

  hidden [void] GitClone([string]$path) {
    $branchArgs = $this.SubstringAfterHash()
    if ($branchArgs -ne '') {
      $branchArgs = "--branch " + $branchArgs
    }
    $repoUrl = $this.SubstringBeforeHash()
    MessageInfo "Git clone $repoUrl"
    $gitCmd = "git clone --quiet -c advice.detachedHead=false --depth 1 $branchArgs $repoUrl $path"
    Invoke-Expression -Command $gitCmd
  }

  hidden [bool] IsGitRemote() {
    $gitUrlRegex = '(https?://*)|^(ssh://*)|^(git://*)|^(git@[^:]+:*)$'
    $myUrl = $this.url
    if ($myUrl -match $gitUrlRegex) {
      return $true;
    } else {
      return $false;
    }
  }

  hidden [string] SubstringBeforeHash() {
    return $this.url.Split('#')[0]
  }

  hidden [string] SubstringAfterHash() {
    return $this.url.Split('#')[1]
  }

  hidden [string] FolderName() {
    $protocols = @('https://', 'ssh://', 'git://', 'git@')
    $trimmedUrl = $this.url
    foreach ($protocol in $protocols) {
      $trimmedUrl = $trimmedUrl -replace [regex]::Escape($protocol), ''
    }
    # replace '/' by '_'
    $folderName = $trimmedUrl -replace '/', '_'
    return $folderName
  }

}

class CmdLineArgs {
  [Array]$args

  CmdLineArgs([Array]$arguments) {
    $this.args = $arguments
  }

  [string] GetSystemProperty([string]$propName) {
    $prefix = "-D$propName="
    foreach ($arg in $this.args) {
      if ($arg -eq $null) {
        continue
      }
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

  [string] GetRemoteBaseDirArg() {
    $remoteArgs= @("-r", "--remote", "-ru", "-ur")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    if ($remoteIndex -eq -1) {
      return $null
    }
    $candidate = $this.args[$remoteIndex + 1]
    if ($candidate -eq "-u" -or $candidate -eq "--update") {
      $candidate = $this.args[$remoteIndex + 2]
    }
    return $candidate
  }

  [array] GetProgramArgs() {
    $index = $this.GetProgramArgIndex() + 1
    return $this.args[$index..$this.args.Length]
  }

  [array] GetPriorProgramArgs() {
    $index = $this.GetProgramArgIndex() - 1
    if ($index -lt 0) {
      return @()
    }
    return $this.args[0..$index]
  }

  [bool] IsUpdateFlagPresent() {
    $remoteArgs= @("-ru", "-ur", "-u", "--update")
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

  [bool] IsCleanFlagPresent() {
    $remoteArgs= @("-c", "--clean")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    return ($remoteIndex -ne -1)
  }

  [array] FilterOutSysProp() {
    $result = @()
    foreach ($item in $this.args) {
      if (!($item.StartsWith("-D") -AND ($item.Contains("="))) ) {
        $result += $item
      }
    }
    return $result
  }

  [array] FilterInSysProp() {
    $result = @()
    foreach ($item in $this.args) {
      if ($item.StartsWith("-D") -AND ($item.Contains("=")) ) {
        $result += $item
      }
    }
    return $result
  }

  hidden [Int16] GetProgramArgIndex() {
    $remoteArgs= @("-p", "--program")
    return $this.GetIndexOfFirstOf($remoteArgs)
  }

}

class Props {
  [CmdLineArgs]$cmdLineArgs
  [string]$baseDir
  [string]$globalPropFile

  Props([CmdLineArgs]$cmdLineArgs, [string]$baseDir, [string]$globalPropFile ) {
    $this.cmdLineArgs = $cmdLineArgs
    $this.baseDir = $baseDir
    $this.globalPropFile = $globalPropFile
  }

  [string] GetValue([string]$propName) {
    $cmdArgsValue = $this.cmdLineArgs.GetSystemProperty($propName)
    if ($cmdArgsValue -ne '') {
      return $cmdArgsValue
    }
    $envValue = [Environment]::GetEnvironmentVariable($propName)
    if ($envValue) {
      return $envValue
    }

    $jekaPropertyFilePath = $this.baseDir + '\jeka.properties'
    $value = [Props]::GetValueFromFile($jekaPropertyFilePath, $propName)
    if ('' -eq $value) {
      $parentDir = $this.baseDir + '\..'
      $parentJekaPropsFile = $parentDir + '\jeka.properties'
      if (Test-Path $parentJekaPropsFile) {
        $parentDirProps = [Props]::new($this.cmdLineArgs, $parentDir, $this.globalPropFile)
        $value = $parentDirProps.GetValue($propName)
      } else {
        $value = [Props]::GetValueFromFile($this.globalPropFile, $propName)
      }
    }
    return $value
  }

  [string] GetValueOrDefault([string]$propName, [string]$defaultValue) {
    $value = $this.GetValue($propName)
    if ($value -eq '') {
      return $defaultValue
    } else {
      return $value
    }
  }

  [CmdLineArgs] InterpolatedCmdLine() {
    $result = @()
    foreach ($arg in $this.cmdLineArgs.args) {
      if ($arg -like "::*") {
        $propKey= ( "jeka.cmd." + $arg.Substring(2) )
        $propValue= $this.GetValue($propKey)
        if ('' -ne $propValue) {
          $valueArray= [Props]::ParseCommandLine($propValue)
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

  static [array] ParseCommandLine([string]$cmdLine) {
    $pattern = '(""[^""]*""|[^ ]*)'
    $regex = New-Object Text.RegularExpressions.Regex $pattern
    return $regex.Matches($cmdLine) | ForEach-Object { $_.Value.Trim('"') } | Where-Object { $_ -ne "" }
  }

  static [string] GetValueFromFile([string]$filePath, [string]$propertyName) {
    if (! [System.IO.File]::Exists($filePath)) {
      return $null
    }
    $data = [System.IO.File]::ReadAllLines($filePath)
    foreach ($line in $data) {
      if ($line -match "^$propertyName=") {
        $keyLength = $propertyName.Length + 1
        $value = $line.Substring($keyLength)
        return $value
      }
    }
    return $null
  }

}

class Jdks {
  [Props]$Props
  [string]$CacheDir

  Jdks([Props]$props, [string]$cacheDir) {
    $this.Props = $props
    $this.CacheDir = $cacheDir
  }

  [string] GetJavaCmd() {
    $javaHome = $this.JavaHome()
    $javaExe = $this.JavaExe($javaHome)
    return $javaExe
  }

  hidden Install([string]$distrib, [string]$version, [string]$targetDir) {
    MessageInfo "Downloading JDK $distrib $version. It may take a while..."
    $jdkurl="https://api.foojay.io/disco/v3.0/directuris?distro=$distrib&javafx_bundled=false&libc_type=c_std_lib&archive_type=zip&operating_system=windows&package_type=jdk&version=$version&architecture=x64&latest=available"
    MessageVerbose "Downloading from $jdkUrl"
    $zipExtractor = [ZipExtractor]::new($jdkurl, $targetDir)
    $zipExtractor.ExtractRootContent()
  }

  hidden [string] CachedJdkDir([string]$version, [string]$distrib) {
    return $this.CacheDir + "\jdks\" + $distrib + "-" + $version
  }

  hidden [string] JavaExe([string]$javaHome) {
    return $javaHome + "\bin\java.exe"
  }

  hidden [string] JavaHome() {
    if ($null -ne $Env:JEKA_JAVA_HOME) {
      return $Env:JEKA_JAVA_HOME
    }
    $version = ($this.Props.GetValueOrDefault("jeka.java.version", "21"))
    $distib = ($this.Props.GetValueOrDefault("jeka.java.distrib", "temurin"))
    $cachedJdkDir =  $this.CachedJdkDir($version, $distib)
    $javaExeFile = $this.JavaExe($cachedJdkDir)
    if (! [System.IO.File]::Exists($javaExeFile)) {
      $this.Install($distib, $version, $cachedJdkDir)
    }
    return $cachedJdkDir
  }

}

class JekaDistrib {
  [Props]$props
  [String]$cacheDir

  JekaDistrib([Props]$props, [string]$cacheDir) {
    $this.props = $props
    $this.cacheDir = $cacheDir
  }

  [string] GetBinDir() {
    $specificLocation = $this.props.GetValue("jeka.distrib.location")
    if ($specificLocation -ne '') {
      return $specificLocation
    }
    $jekaVersion = $this.props.GetValue("jeka.version")

    # If version not specified, use jeka jar present in running distrib
    if ($jekaVersion -eq '' -or $jekaVersion -eq '.') {
      $dir = $PSScriptRoot
      $jarFile = $dir + "\dev.jeka.jeka-core.jar"
      if (! [System.IO.File]::Exists($jarFile)) {
        Write-Host "No Jeka jar file found at $jarFile" -ForegroundColor Red
        Write-Host "This is due that neither jeka.distrib.location nor jeka.version are specified in properties, " -ForegroundColor Red
        Write-Host "and you are probably invoking local 'jeka.ps1'" -ForegroundColor Red
        Write-Host "Specify one the mentionned above properties or invoke 'jeka' if JeKa is installed on host machine." -ForegroundColor Red
        exit 1
      }
      return $dir
    }

    $distDir = $this.cacheDir + "\distributions\" + $jekaVersion
    if ( [System.IO.Directory]::Exists($distDir)) {
      return "$distDir\bin"
    }

    $distRepo = $this.props.GetValueOrDefault("jeka.distrib.repo", "https://repo.maven.apache.org/maven2")
    $url = "$distRepo/dev/jeka/jeka-core/$jekaVersion/jeka-core-$jekaVersion-distrib.zip"
    $zipExtractor = [ZipExtractor]::new($url, $distDir)
    $zipExtractor.Extract()
    return "$distDir\bin"
  }

  [string] GetJar() {
    return $this.GetBinDir() + "\dev.jeka.jeka-core.jar"
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
    $root = "$tempDir\$($subDirs[0])"
    MessageVerbose "Copying downloaded JDK $root to $($this.dir)"
    if (-not (Test-Path -Path $this.dir)) {
      New-Item -ItemType Directory -Path $this.dir
    }
    Move-Item -Path "$root\*" -Destination $this.dir -Force
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
    try {
        $webClient.DownloadFile($this.url, $downloadFile)
    } catch {
      $msg = "Error while downloading : " + $this.url
      Write-Error $msg
      Exit-Error "$($_.Exception.Message)"
    }
    $webClient.Dispose()
    return $downloadFile
  }
}

class ProgramExecutor {
  [string]$folder
  [array]$cmdLineArgs

  ProgramExecutor([string]$folder, [array]$cmdLineArgs) {
    $this.folder = $folder
    $this.cmdLineArgs = $cmdLineArgs
  }

  Exec([string]$javaCmd, [string]$progFile) {
    if ($progFile.EndsWith('.exe')) {
      & "$progFile" $this.cmdLineArgs
    } else {
      & "$javaCmd" -jar "$progFile" $this.cmdLineArgs
    }
  }

  [string] FindProg() {
    $dir = $this.folder
    if (-not (Test-Path $dir)) {
      return $null
    }
    $exeFile = $this.findFile(".exe")
    if ($exeFile -ne '') {
      return $exeFile
    }
    return $this.findFile(".jar")
  }

  hidden [string] findFile([string]$extension) {
    $files = Get-ChildItem -Path $this.folder -Filter "*$extension"
    if ($files) {
      $firstFile = $files | Select-Object -First 1
      return $firstFile.FullName
    }
    return $null
  }
}

function ExecJekaEngine {
  param(
    [string]$baseDir,
    [string]$cacheDir,
    [Props]$props,
    [string]$javaCmd,
    [array]$cmdLineArgs,
    [Parameter(Mandatory=$false)][bool]$stderr = $false
  )

  $jekaDistrib = [JekaDistrib]::new($props, $cacheDir)
  $jekaJar = $jekaDistrib.GetJar()
  $classpath = "$baseDir\jeka-boot\*;$jekaJar"
  $jekaOpts = $Env:JEKA_OPTS
  $baseDirProp = "-Djeka.current.basedir=$baseDir"
  & "$javaCmd" $jekaOpts "$baseDirProp" -cp "$classpath" "dev.jeka.core.tool.Main" $cmdLineArgs
}

function ExecProg {
  param(
    [string]$javaCmd,
    [string]$progFile,
    [array]$cmdLineArgs
  )

  $argLine = $cmdLineArgs -join ' '
  if ($progFile.EndsWith('.exe')) {
    Write-Verbose "Run native program $progFile with args $argLine"
    & "$progFile" $cmdLineArgs
  } else {
    $cmdLineArgs = [CmdLineArgs]::new($cmdLineArgs)
    $sypPropArgs = $cmdLineArgs.FilterInSysProp()
    $sanitizedProgArgs = $cmdLineArgs.FilterOutSysProp()
    Write-Verbose "Run Java program $progFile with args $argLine"
    & "$javaCmd" -jar "$sypPropArgs" "$progFile" $sanitizedProgArgs
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
  if (($cmdLineArgs.IsCleanFlagPresent()) -and (Test-Path .\jeka-output)) {
    Remove-Item .\jeka-output -Recurse -Force -ErrorAction SilentlyContinue
  }

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
        $buildCmd = "project: pack pack.jarType=FAT pack.detectMainClass=true -Djeka.test.skip=true --stderr"
      } else {
        $buildCmd = "base: pack -Djeka.test.skip=true --stderr"
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