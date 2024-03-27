function Write-Message {
  param([string]$msg)

  if ($global:Verbose) {
    Write-Host $msg
  }
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
  [string]$updateFlag

  BaseDirResolver([string]$url, [string]$cacheDir, [bool]$updateFlag) {
    $this.url = $url
    $this.cacheDir = $cacheDir
    $this.updateFlag = $updateFlag
  }

  [string] GetPath() {
    if ($this.url -eq '') {
      return $PWD.Path
    }
    if (! $this.IsGitRemote()) {
      return $this.url
    }
    $path = $this.cacheDir + "\git\" + $this.FolderName()
    if ([System.IO.Directory]::Exists($path)) {
      if ($this.updateFlag) {
        Remove-Item -Path $path -Recurse -Force
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
    $gitCmd = "git clone --quiet -c advice.detachedHead=false --depth 1 $branchArgs $repoUrl $path"
    Invoke-Expression -Command $gitCmd
  }

  hidden [bool] IsGitRemote() {
    $gitUrlRegex = '(https?://.+.git*)|^(ssh://.+.git*)|^(git://.+.git*)|^(git@[^:]+:.+.git*)$'
    if ($this.url -match $gitUrlRegex) {
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
    return $this.args[$remoteIndex + 1]
  }

  [bool] IsUpdateFlagPresent() {
    $remoteArgs= @("-ru", "-ur", "-u", "--remote-update")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    return ($remoteIndex -ne -1)
  }

  [bool] IsVerboseFlagPresent() {
    $remoteArgs= @("-v", "--verbose", "--debug")
    $remoteIndex= $this.GetIndexOfFirstOf($remoteArgs)
    return ($remoteIndex -ne -1)
  }

  [bool] IsProgramFlagPresent() {
    $remoteArgs= @("-p", "--program")
    $remoteIndex= Get-indexOfFirst($this.args,$remoteArgs)
    return ($remoteIndex -ne -1)
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
    if ($null -ne $envValue) {
      return $envValue
    }
    $jekaPropertyFilePath = $this.baseDir + '\jeka.properties'

    $value = [Props]::GetValueFromFile($jekaPropertyFilePath, $propName)
    if ($null -eq $value) {
      $parentDir = $this.baseDir + '\..'
      $parentJekaPropsFile = $parentDir + '\jeka.properties'
      if (Test-Path $parentJekaPropsFile) {
        $value = $this.GetValueFromFile($parentJekaPropsFile, $propName)
      } else {
        $value = $this.GetValueFromFile($this.globalPropFile, $propName)
      }
    }
    return $value
  }

  [string] GetValueOrDefault([string]$propName, [string]$defaultValue) {
    $value = $this.GetValue($propName)
    $isNull = ($value -eq '')
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
        if ($null -ne $propValue) {
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

  static [string] ParseCommandLine([string]$cmdLine) {
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
        $value = $line.Split('=')[1]
        return $value.Trim()
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
    Write-Message "Downloading JDK $distrib $version. It may take a while..."
    $jdkurl="https://api.foojay.io/disco/v3.0/directuris?distro=$distrib&javafx_bundled=false&libc_type=c_std_lib&archive_type=zip&operating_system=windows&package_type=jdk&version=$version&architecture=x64&latest=available"
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
    if ($Env:JEKA_JAVA_HOME -ne $null) {
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

  [string] GetDir() {
    $specificLocation = $this.props.GetValue("jeka.distrib.location")
    if ($specificLocation -ne '') {
      return $specificLocation
    }
    $jekaVersion = $this.props.GetValue("jeka.version")

    # If version not specified, use jeka jar present in running distrib
    if ($jekaVersion -eq '') {
      $dir = $PSScriptRoot
      $jarFile = $dir + "\dev.jeka.jeka-core.jar"
      if (! [System.IO.File]::Exists($jarFile)) {
        Write-Host "No Jeka jar file found at $jarFile"
        Write-Host "This is due that neither jeka.distrib.location nor jeka.version are specified in properties, "
        Write-Host "and you are probably invoking local 'jeka.ps1'"
        Write-Host "Specify one the mentionned above properties or invoke 'jeka' if JeKa is installed on host machine."
        exit 1
      }
      return $dir
    }

    $distDir = $this.cacheDir + "\distributions\" + $jekaVersion
    if ( [System.IO.Directory]::Exists($distDir)) {
      return $distDir
    }

    $distRepo = $this.props.GetValueOrDefault("jeka.distrib.repo", "https://repo.maven.apache.org/maven2")
    $url = "$distRepo/dev/jeka/jeka-core/$jekaVersion/jeka-core-$jekaVersion-distrib.zip"
    $zipExtractor = [ZipExtractor]::new($url, $distDir)
    $zipExtractor.Extract()
    return $distDir
  }

  [string] GetJar() {
    return $this.GetDir() + "\dev.jeka.jeka-core.jar"
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

  $jekaUserHome = Get-JekaUserHome
  $cacheDir = Get-CacheDir($jekaUserHome)
  $globalPropFile = $jekaUserHome + "\global.properties"

  # Get interpolated cmdLine, while ignoring Base dir
  $rawCmdLineArgs = [CmdLineArgs]::new($arguments)
  $rawProps = [Props]::new($rawCmdLineArgs, $PWD.Path, $globalPropFile)
  $cmdLineArgs = $rawProps.InterpolatedCmdLine()

  # Resolve basedir and interpolate cmdLine according props declared in base dir
  $remoteArg = $cmdLineArgs.GetRemoteBaseDirArg()
  $updateArg = $cmdLineArgs.IsUpdateFlagPresent()
  $baseDirResolver = [BaseDirResolver]::new($remoteArg, $cacheDir, $updateArg)
  $baseDir = $baseDirResolver.GetPath()
  $props = [Props]::new($cmdLineArgs, $baseDir, $globalPropFile)
  $cmdLineArgs = $props.InterpolatedCmdLine()
  $global:Verbose = $cmdLineArgs.IsVerboseFlagPresent()

  # Compute Java command
  $jdks = [Jdks]::new($props, $cacheDir)
  $javaCmd = $jdks.GetJavaCmd()

  if ($cmdLineArgs.IsUpdateFlagPresent()) {
    # Try to Execute program without passing by Jeka
  } else {
    $jekaDistrib = [JekaDistrib]::new($props, $cacheDir)
    $jekaJar = $jekaDistrib.GetJar()
    $classpath = "$baseDir\jeka-boot\*;$jekaJar"
    $jekaOpts = $Env:JEKA_OPTS
    $baseDirProp = "-Djeka.current.basedir=$baseDir"
    & "$javaCmd" "$jekaOpts" $baseDirProp -cp $classpath "dev.jeka.core.tool.Main" $arguments
  }

}

$ErrorActionPreference = "Stop"
Main -arguments $args