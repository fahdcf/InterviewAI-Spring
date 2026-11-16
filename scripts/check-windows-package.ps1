param(
    [Parameter(Mandatory)]
    [string]$Archive
)

$ErrorActionPreference = 'Stop'
$archivePath = (Resolve-Path -LiteralPath $Archive).Path
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$extracted = Join-Path $tempRoot ('InterviewAI package check ' + [guid]::NewGuid().ToString('N'))
$launcher = Join-Path $extracted 'InterviewAI\InterviewAI.exe'
$runtime = Join-Path $extracted 'InterviewAI\runtime'
$originalPath = $env:Path
$originalJavaHome = $env:JAVA_HOME

try {
    Expand-Archive -LiteralPath $archivePath -DestinationPath $extracted
    $env:Path = "$env:SystemRoot\System32;$env:SystemRoot"
    $env:JAVA_HOME = $null
    if (Get-Command java.exe -ErrorAction SilentlyContinue) {
        throw 'An external java.exe is still on PATH.'
    }

    # The launcher may start a child process. Check the process that owns the window.
    Start-Process -FilePath $launcher -WorkingDirectory $extracted -WindowStyle Hidden
    for ($attempt = 0; $attempt -lt 50; $attempt++) {
        $app = Get-Process -Name InterviewAI -ErrorAction SilentlyContinue |
            Where-Object { $_.Path -eq $launcher -and $_.MainWindowTitle -eq 'InterviewAI' } |
            Select-Object -First 1
        if ($app) {
            $jvm = $app.Modules | Where-Object ModuleName -eq 'jvm.dll' | Select-Object -First 1
            if ($jvm -and $jvm.FileName.StartsWith("$runtime\", [StringComparison]::OrdinalIgnoreCase)) {
                Write-Output 'InterviewAI opened its sign-in window using the bundled Java runtime.'
                return
            }
        }
        Start-Sleep -Milliseconds 500
    }
    throw 'The extracted application did not open its sign-in window with the bundled Java runtime.'
} finally {
    $env:Path = $originalPath
    $env:JAVA_HOME = $originalJavaHome
    Get-Process -Name InterviewAI -ErrorAction SilentlyContinue |
        Where-Object Path -eq $launcher |
        Stop-Process -PassThru -ErrorAction SilentlyContinue |
        Wait-Process -Timeout 5
    if (Test-Path -LiteralPath $extracted) {
        $verifiedPath = (Resolve-Path -LiteralPath $extracted).Path
        if ((Split-Path -Parent $verifiedPath) -eq $tempRoot.TrimEnd('\') -and $verifiedPath -eq $extracted) {
            for ($attempt = 0; $attempt -lt 5; $attempt++) {
                try {
                    Remove-Item -LiteralPath $verifiedPath -Recurse -Force
                    break
                } catch {
                    if ($attempt -eq 4) { throw }
                    Start-Sleep -Seconds 1
                }
            }
        }
    }
}
