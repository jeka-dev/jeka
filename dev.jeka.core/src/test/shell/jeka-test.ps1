. ".\..\..\main\shell\jeka.ps1"

function TestInterpolate([string]$baseDir, [Array]$cmdLineArgs) {
    $interpolated = Get-InterpolatedCmd "sample-dir\sub-dir" $cmdLineArgs
    Write-Output ([System.String]::Join(' ', $cmdLineArgs) + " -> " + [System.String]::Join(' ', $interpolated))
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
Write-Output "-------- CmdLineArgs -----------"
$rawArgs = @( "toto", "-u", "-Dfoo=bar2" )
$cmdLineArgs = [CmdLineArgs]::new($rawArgs)
Write-Output $cmdLineArgs.GetSystemProperty("foo")
$args = @("-u", "--update")
Write-Output $cmdLineArgs.GetIndexOfFirstOf($args)
$cmdLineArgs = @( "toto", "uu", "-Dfoo=bar2" )
TestInterpolate  "sample-dir\sub-dir" $cmdLineArgs
Write-Output "--"
$cmdLineArgs = @( "toto", "::uu", "-Dfoo=bar2", ":bb", "-Djeka.cmd.uu=coco popo" )
$baseDir = $PWD.Path + "\sample-dir\sub-dir"
TestInterpolate $baseDir $cmdLineArgs
Write-Output ""
Write-Output "--------Props -----------"
$file = $PWD.Path + "\sample-dir\jeka.properties"
Write-Output ("foo=" +  [Props]::GetValueFromFile($file, "foo"))
$cmdLineArgs = [CmdLineArgs]::new(@("toto", "uu", "-Dfoo=bar2"))
$baseDir =  $PWD.Path + ("\sample-dir\sub-dir")

$props = [Props]::new($cmdLineArgs, $baseDir)

Write-Output ("overriden.prop=" + $props.GetValue("overriden.prop"))
Write-Output ("jeka.java.version=" +  $props.GetValue("jeka.java.version"))
Write-Output ("non.existing=" +  $props.GetValue("non.existing"))

Write-Output ""
Write-Output "-------- IsGitRemote -----------"
Write-Output (IsGitRemote "../ooop/uu")
Write-Output (IsGitRemote "https://ooop/uu.git")
Write-Output (IsGitRemote "https://ooop/uu.git#foo")
Write-Output ""
Write-Output "-------- Folder from git repo -----------"
Write-Output (Get-FolderNameFromGitUrl "https://ooop/uu.git#foo")