param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [ValidateSet('home-scroll','settings-scroll','tab-cycle','music-scroll','player-enter','player-playback')]
    [string]$Scenario,
    [int]$Rounds = 3,
    [string]$OutputDirectory = 'build/perf'
)

$ErrorActionPreference = 'Stop'
$adb = 'D:\programming\devtools\android\sdk\platform-tools\adb.exe'
$package = 'com.textvision.alistclient'

function Invoke-Adb([string[]]$Arguments) {
    & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')" }
}

function Invoke-Swipe([int]$x1, [int]$y1, [int]$x2, [int]$y2, [int]$duration = 250) {
    Invoke-Adb @('shell','input','swipe',"$x1","$y1","$x2","$y2","$duration")
}

function Invoke-Scenario([string]$Name) {
    switch ($Name) {
        'home-scroll' {
            Invoke-Adb @('shell','input','tap','132','2460')
            Start-Sleep -Milliseconds 500
            1..5 | ForEach-Object {
                Invoke-Swipe 600 2050 600 500
                Invoke-Swipe 600 500 600 2050
            }
        }
        'settings-scroll' {
            Invoke-Adb @('shell','input','tap','1065','2460')
            Start-Sleep -Milliseconds 500
            1..5 | ForEach-Object {
                Invoke-Swipe 600 2050 600 500
                Invoke-Swipe 600 500 600 2050
            }
        }
        'tab-cycle' {
            1..3 | ForEach-Object {
                132,365,600,833,1065,132 | ForEach-Object {
                    Invoke-Adb @('shell','input','tap',"$_",'2460')
                    Start-Sleep -Milliseconds 250
                }
            }
        }
        'music-scroll' {
            Invoke-Adb @('shell','input','tap','600','2460')
            Start-Sleep -Milliseconds 500
            1..5 | ForEach-Object {
                Invoke-Swipe 600 2050 600 550
                Invoke-Swipe 600 550 600 2050
            }
        }
        'player-enter' {
            Invoke-Adb @('shell','input','tap','600','2460')
            Start-Sleep -Milliseconds 500
            Invoke-Adb @('shell','input','tap','450','550')
            Start-Sleep -Milliseconds 300
            Invoke-Adb @('shell','input','tap','600','875')
            Start-Sleep -Milliseconds 800
        }
        'player-playback' {
            Invoke-Adb @('shell','input','tap','600','2460')
            Start-Sleep -Milliseconds 500
            Invoke-Adb @('shell','input','tap','450','550')
            Start-Sleep -Milliseconds 300
            Invoke-Adb @('shell','input','tap','600','875')
            Start-Sleep -Milliseconds 800
            1..8 | ForEach-Object {
                Invoke-Adb @('shell','input','tap','600','2080')
                Start-Sleep -Milliseconds 500
            }
        }
        default {
            throw "$Name requires UI-node coordinates captured in the baseline document before running"
        }
    }
}

if (-not (Test-Path -LiteralPath $adb)) { throw "missing adb: $adb" }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
1..$Rounds | ForEach-Object {
    Invoke-Adb @('shell','am','force-stop',$package) | Out-Null
    Invoke-Adb @('shell','am','start','-n',"$package/.MainActivity") | Out-Null
    Start-Sleep -Milliseconds 800
    Invoke-Adb @('shell','dumpsys','gfxinfo',$package,'reset') | Out-Null
    Invoke-Scenario $Scenario
    $result = Invoke-Adb @('shell','dumpsys','gfxinfo',$package)
    $result | Set-Content -Encoding utf8 (Join-Path $OutputDirectory "$Scenario-$_.txt")
}
