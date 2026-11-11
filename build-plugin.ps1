# Run from PowerShell or by double-clicking build-plugin.bat.
# Downloads official Gradle, resolves official IntelliJ SDK, and builds the installable ZIP.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$project = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $project

$gradleVersion = '9.2.1'
$cache = Join-Path $project '.gradle-tools'
$gradleHome = Join-Path $cache ("gradle-" + $gradleVersion)
$gradle = Join-Path $gradleHome 'bin\gradle.bat'

Write-Host 'Timestamp & UUID Generator 1.2.0 - official IntelliJ SDK build' -ForegroundColor Cyan
if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java not found. Install JDK 21 and set JAVA_HOME/PATH, then retry.'
}
& java -version
if ($LASTEXITCODE -ne 0) { throw 'Java cannot start.' }

if (!(Test-Path $gradle)) {
    New-Item -ItemType Directory -Path $cache -Force | Out-Null
    $archive = Join-Path $cache ("gradle-" + $gradleVersion + '-bin.zip')
    $uri = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    Write-Host "Downloading official Gradle $gradleVersion..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $uri -OutFile $archive -UseBasicParsing
    Write-Host 'Extracting Gradle...'
    Expand-Archive -Path $archive -DestinationPath $cache -Force
    Remove-Item -LiteralPath $archive -Force
}

Write-Host 'Building with the JetBrains IntelliJ Platform Gradle plugin...' -ForegroundColor Cyan
& $gradle clean test buildPlugin --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Build failed. Review the Gradle output above.' }

$distribution = Get-ChildItem -Path (Join-Path $project 'build\distributions') -Filter '*.zip' -File |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $distribution) { throw 'Build completed but no plugin ZIP was found.' }
Write-Host ''
Write-Host 'SUCCESS - install this ZIP using Settings > Plugins > Install Plugin from Disk:' -ForegroundColor Green
Write-Host $distribution.FullName -ForegroundColor Green
