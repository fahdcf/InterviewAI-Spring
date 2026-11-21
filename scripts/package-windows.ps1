$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'Build the Windows application image on Windows.'
}

$root = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $root 'mvnw.cmd'
$release = Join-Path $root ('target/distribution-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$input = Join-Path $release 'desktop-input'
$desktopJar = 'interviewai-desktop-1.0.0.jar'
$backendJar = 'interviewai-backend-1.0.0.jar'

Push-Location -LiteralPath $root
try {
    & $wrapper -B -ntp package
    if ($LASTEXITCODE -ne 0) {
        throw 'The Maven build failed.'
    }

    New-Item -ItemType Directory -Path $input | Out-Null
    & $wrapper -B -ntp -pl desktop dependency:copy-dependencies '-DincludeScope=runtime' "-DoutputDirectory=$input"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not copy the desktop runtime dependencies.'
    }
    Copy-Item -LiteralPath "desktop/target/$desktopJar" -Destination $input

    & jpackage --type app-image --name InterviewAI --app-version 1.0.0 `
        --input $input --dest $release --main-jar $desktopJar `
        --main-class com.interviewai.desktop.DesktopLauncher `
        --add-modules java.base,java.desktop,java.logging,java.net.http,java.scripting,java.xml,jdk.crypto.ec,jdk.unsupported
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not create the Windows application image.'
    }

    Copy-Item -LiteralPath "backend/target/$backendJar" -Destination $release
    $desktopFolder = Join-Path $release 'InterviewAI'
    $zip = Join-Path $release 'InterviewAI-windows.zip'
    Compress-Archive -LiteralPath $desktopFolder -DestinationPath $zip
    $checksumFile = Join-Path $release 'SHA256SUMS'
    Get-FileHash -Algorithm SHA256 -LiteralPath $zip,(Join-Path $release $backendJar) |
        ForEach-Object { "$($_.Hash.ToLowerInvariant())  $([IO.Path]::GetFileName($_.Path))" } |
        Set-Content -LiteralPath $checksumFile -Encoding ascii
    Write-Output "Desktop archive: $zip"
    Write-Output "Backend JAR: $(Join-Path $release $backendJar)"
} finally {
    Pop-Location
}
