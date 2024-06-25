
$gitUrlRegex = '(https?://.+.git*)|^(ssh://.+.git*)|^(git://.+.git*)|^(git@[^:]+:.+.git*)$'
$myUrl = "https://github.com/jeka-dev/demo-cowsay"

if ($myUrl -match $gitUrlRegex) {
    Write-Host "match"
} else {
    Write-Host "_____$myUrl------not remote"
}


