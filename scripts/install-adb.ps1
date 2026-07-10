# Install Rotation Control debug APK via adb
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apk)) {
    Write-Host "Building debug APK..."
    Push-Location $root
    try {
        & .\gradlew.bat assembleDebug
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $apk)) {
    throw "APK not found: $apk"
}

Write-Host "Installing $apk"
adb install -r $apk
Write-Host "Done. Open the app, enable Accessibility + Modify system settings, then set per-app rules."
