#Requires -Version 5

# Entry point for developers, AI agents, and CI to build and run JeKa purely from source,
# with no prior JeKa or build tool installation required.
#
# It compiles JeKa from scratch using only the JDK, then hands off to the compiled JeKa
# to execute the given arguments (e.g. 'project: pack', 'base: test').
#
# Usage: .\bootstrap.ps1 <jeka arguments>
# Example: .\bootstrap.ps1 project: pack --clean --inspect --stacktrace

$ErrorActionPreference = "Stop"

function Get-Files {
  param(
    [string[]]$Paths,
    [string]$Filter = "*",
    [switch]$File
  )

  $result = @()
  foreach ($path in $Paths) {
    if (Test-Path -LiteralPath $path) {
      $items = Get-ChildItem -LiteralPath $path -Recurse -Filter $Filter -File:$File
      $result += $items
    }
  }
  return $result
}

function Get-Classpath {
  param([System.IO.FileInfo[]]$Jars)

  return ($Jars | ForEach-Object { $_.FullName }) -join [System.IO.Path]::PathSeparator
}

function Assert-NativeCommandSucceeded {
  param([string]$Command)

  if ($LASTEXITCODE -ne 0) {
    throw "$Command failed with exit code $LASTEXITCODE."
  }
}

function Get-RelativePath {
  param(
    [string]$Root,
    [string]$Path
  )

  $rootFullPath = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
  $pathFullPath = [System.IO.Path]::GetFullPath($Path)
  return $pathFullPath.Substring($rootFullPath.Length)
}

Push-Location -LiteralPath $PSScriptRoot
try {
  $bin = ".jeka-work\bootstrap"

  # JARs needed to compile JeKa from scratch
  $compileJars = Get-Files -Paths @("core\jeka-boot", "core\libs\compile-only") -Filter "*.jar" -File
  $compileCp = Get-Classpath -Jars $compileJars

  Write-Host "Preparing output directory $bin..."
  $binParent = Split-Path -Parent $bin
  if ($binParent -and !(Test-Path -LiteralPath $binParent)) {
    $null = New-Item -ItemType Directory -Path $binParent
  }
  if (Test-Path -LiteralPath $bin) {
    Remove-Item -LiteralPath $bin -Recurse -Force
  }
  $null = New-Item -ItemType Directory -Path $bin

  Write-Host "Compiling JeKa sources..."
  $sourcesFile = [System.IO.Path]::GetTempFileName()
  try {
    $sourceFiles = Get-Files -Paths @("core\src\main\java", "core\jeka-src", "plugins\plugins.nexus") -Filter "*.java" -File
    $sourceLines = $sourceFiles | ForEach-Object {
      '"' + ($_.FullName.Replace('\', '/') -replace '"', '\"') + '"'
    }
    [System.IO.File]::WriteAllLines($sourcesFile, [string[]]$sourceLines, [System.Text.UTF8Encoding]::new($false))
    $sourceCount = $sourceFiles.Count

    $sourcesArg = "@$sourcesFile"
    & javac -encoding UTF-8 --release 21 -d $bin -cp $compileCp $sourcesArg
    Assert-NativeCommandSucceeded "javac"
  }
  finally {
    if (Test-Path -LiteralPath $sourcesFile) {
      Remove-Item -LiteralPath $sourcesFile -Force
    }
  }
  Write-Host "$sourceCount source files compiled."

  Write-Host "Copying resources..."
  $resourceRoot = "core\src\main\java"
  $resourceFiles = Get-ChildItem -LiteralPath $resourceRoot -Recurse -File | Where-Object { $_.Name -notlike "*.java" }
  foreach ($file in $resourceFiles) {
    $relativePath = Get-RelativePath -Root $resourceRoot -Path $file.FullName
    $destination = Join-Path $bin $relativePath
    $destinationDir = Split-Path -Parent $destination
    if ($destinationDir -and !(Test-Path -LiteralPath $destinationDir)) {
      $null = New-Item -ItemType Directory -Path $destinationDir -Force
    }
    Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
  }

  Write-Host "Launching JeKa..."
  $testJars = Get-Files -Paths @("core\libs\test") -Filter "*.jar" -File
  $runtimeClasspath = (@($bin) + ($compileJars | ForEach-Object { $_.FullName }) + ($testJars | ForEach-Object { $_.FullName })) -join [System.IO.Path]::PathSeparator
  & java -cp $runtimeClasspath dev.jeka.core.tool.Main $args
  Assert-NativeCommandSucceeded "java"
}
finally {
  Pop-Location
}
