. ".\..\..\main\shell\jeka.ps1"

function TestInterpolate {
    param(
        [string]$baseDir,
        [Array]$cmdLineArgs)
    $interpolated = Get-InterpolatedCmd "sample-dir\sub-dir" $cmdLineArgs
    foreach ($item in $cmdLineArgs) {
        Write-Output $item
    }
    Write-Output "->"
    foreach ($item in $interpolated) {
        Write-Output $item
    }
}

Write-Output "-------- Some computed constants -----------"
$jekaUserHome = Get-JekaUserHome
Write-Output ("jeka user hme :" +  $jekaUserHome)
$cacheDir = Get-CacheDir
Write-Output ("Cache dir : " + $cacheDir)
Write-Output ""
Write-Output "-------- String manipulation -----------"
Write-Output (Get-SubstringBeforeHash "rrr#ooo")
Write-Output (Get-SubstringAfterHash "rrr#ooo")
Write-Output ""
Write-Output "-------- Find properties -----------"
$cmdArgValuepropValue = Get-PropValueFromFile "sample-dir/jeka.properties" "foo"
Write-Output ("foo=" +  $propValue)
$cmdLineArgs = @("toto", "uu", "-Dfoo=bar2")
$cmdArgValue = Get-SystemPropFromArgs $cmdLineArgs "foo"
Write-Output ("foo=" +  $cmdArgValue)
$overridenValue= Get-PropValueFromBaseDir "sample-dir\sub-dir" $cmdArgValue  "overriden.prop"
Write-Output ("overriden.prop=" +  $overridenValue)
$javaVersionValue= Get-PropValueFromBaseDir "sample-dir\sub-dir" $cmdArgValue  "jeka.java.version"
Write-Output ("jeka.java.version=" +  $javaVersionValue)
$nonExistingValue= Get-PropValueFromBaseDir "sample-dir\sub-dir" $cmdArgValue  "non.existing"
Write-Output ("non.existing=" +  $nonExistingValue)
Write-Output ""
Write-Output "-------- Interpolate -----------"
$parsed = Get-ParsedCommandLine "toto tutu"
foreach ($item in $parsed) {
    Write-Output $item
}
Write-Output "------"
$cmdLineArgs = @( "toto", "uu", "-Dfoo=bar2" )
TestInterpolate  "sample-dir\sub-dir" $cmdLineArgs
Write-Output "------"
$cmdLineArgs = @( "toto", "::uu", "-Dfoo=bar2", ":bb", "-Djeka.cmd.uu=coco popo" )
TestInterpolate  "sample-dir\sub-dir" $cmdLineArgs


