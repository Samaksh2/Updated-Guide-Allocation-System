# Downloads the MongoDB Java driver's runtime JARs into ../lib.
# Run once from PowerShell: .\scripts\setup-mongodb-driver.ps1
$ErrorActionPreference = "Stop"
$driverVersion = "5.2.1"
$libraryDirectory = Join-Path $PSScriptRoot "..\lib"
$repository = "https://repo1.maven.org/maven2/org/mongodb"
$artifacts = @("mongodb-driver-sync", "mongodb-driver-core", "bson", "bson-record-codec")

New-Item -ItemType Directory -Force -Path $libraryDirectory | Out-Null
foreach ($artifact in $artifacts) {
    $destination = Join-Path $libraryDirectory "$artifact-$driverVersion.jar"
    Invoke-WebRequest "$repository/$artifact/$driverVersion/$artifact-$driverVersion.jar" -OutFile $destination
}
Write-Host "MongoDB Java driver installed in $libraryDirectory"
