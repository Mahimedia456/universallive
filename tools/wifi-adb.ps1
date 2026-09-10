param(
    [Parameter(Mandatory=$true)][string]$PhoneIp,
    [Parameter(Mandatory=$true)][int]$PairPort,
    [Parameter(Mandatory=$true)][int]$AdbPort
)

Write-Host "Pairing with $PhoneIp`:$PairPort ..."
adb pair "$PhoneIp`:$PairPort"
Write-Host "Connecting to $PhoneIp`:$AdbPort ..."
adb connect "$PhoneIp`:$AdbPort"
Write-Host "Connected devices:"
adb devices
