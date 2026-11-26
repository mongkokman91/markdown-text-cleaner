# Update only the existing app on a specifically selected, already-authorized device.
# No pairing, app removal, downgrade, networking changes, or device discovery scans.
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][string]$Serial,
    [Parameter(Mandatory)][string]$Apk,
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{64}$')][string]$Sha256,
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{64}$')][string]$SignerSha256,
    [Parameter(Mandatory)][ValidateRange(2, 2100000000)][int]$VersionCode,
    [Parameter(Mandatory)][string]$BuildToolsPath
)
$ErrorActionPreference = 'Stop'
$package = 'com.example.markdowntextcleaner'

function Invoke-Checked([string]$Exe, [string[]]$Arguments) {
    $result = & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed (exit $LASTEXITCODE)." }
    return ($result -join "`n")
}
function Get-VersionCode {
    $dump = Invoke-Checked adb @('-s', $Serial, 'shell', 'dumpsys', 'package', $package)
    if ($dump -notmatch '(?m)^\s*versionCode=(\d+)\b') {
        throw 'Existing app not found. No fresh install or uninstall performed.'
    }
    return [int]$Matches[1]
}

$apkPath = (Resolve-Path -LiteralPath $Apk).ProviderPath
$actualHash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash
if ($actualHash -ne $Sha256) { throw 'APK hash does not match the promoted build.' }

$signer = Join-Path $BuildToolsPath 'apksigner.bat'
$info = Invoke-Checked $signer @('verify', '--print-certs', $apkPath)
$certs = [Regex]::Matches($info, '(?im)^Signer #\d+ certificate SHA-256 digest:\s*([a-f0-9]{64})\s*$')
if ($certs.Count -ne 1 -or $certs[0].Groups[1].Value -ne $SignerSha256) {
    throw 'APK did not verify against the pinned certificate.'
}
$analyzer = Join-Path $BuildToolsPath 'aapt.exe'
$badging = Invoke-Checked $analyzer @('dump', 'badging', $apkPath)
if ($badging -notmatch "(?m)^package: name='([^']+)' versionCode='(\d+)'") {
    throw 'Unable to read APK package and version.'
}
if ($Matches[1] -cne $package -or [int]$Matches[2] -ne $VersionCode) {
    throw 'Unexpected APK package or version.'
}

$state = Invoke-Checked adb @('-s', $Serial, 'get-state')
if ($state.Trim() -ne 'device') { throw 'The selected phone is not authorized and reachable.' }
$model = Invoke-Checked adb @('-s', $Serial, 'shell', 'getprop', 'ro.product.model')
if ($model.Trim() -ne 'PJD110') { throw "Not the expected OnePlus 12 (PJD110)." }
$current = Get-VersionCode
if ($current -eq $VersionCode) {
    Write-Output "Version $VersionCode is already installed."
    return
}
if ($current -gt $VersionCode) { throw 'Refusing to downgrade.' }
if ($PSCmdlet.ShouldProcess("$package on the selected OnePlus 12", "Install version $VersionCode in place")) {
    # Android itself also rejects upgrades with an incompatible signing key.
    $status = Invoke-Checked adb @('-s', $Serial, 'install', '-r', '--no-streaming', $apkPath)
    if ($status -notmatch '(?m)^Success\s*$' -or (Get-VersionCode) -ne $VersionCode) {
        throw 'Could not verify the installed version.'
    }
    Write-Output "Verified installed version $VersionCode."
}
