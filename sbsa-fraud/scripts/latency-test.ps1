<#
.SYNOPSIS
  Measures fraud-decision latency for NFR-01 (95th percentile at most 200 ms).
.DESCRIPTION
  Start the application first (gradlew bootRun), then run:
      powershell -ExecutionPolicy Bypass -File scripts\latency-test.ps1 -Count 200
  Sends -Count transactions across 40 seeded accounts, reads processingMs from each
  response (time spent inside the service) and also times each HTTP round trip.
  Saves every measurement to scripts\results\latency-<time>.csv.
#>
param([int]$Count = 200, [string]$BaseUrl = "http://localhost:8080", [int]$TargetP95Ms = 200)

$ErrorActionPreference = "Stop"
$accounts = @('4128728463','4141227216','4147338124','4155176955','4171662963','4119335534','4161019678','4196977837','4195899313','4114308421','4163100814','4188320463','4130514014','4181016525','4149392920','4178139880','4131682744','4151273847','4181498611','4182383095','4126240908','4119528530','4175151231','4173481353','4117270733','4181979055','4169476001','4141726318','4132097220','4184593961','4118181586','4120752378','4186460539','4152462441','4171367643','4138609087','4182909480','4128024248','4156020613','4147135391')
$runId    = Get-Date -Format "MMddHHmmss"
$now      = (Get-Date).ToUniversalTime()
$rows     = New-Object System.Collections.Generic.List[object]

function Pct([double[]]$values, [double]$p) {
    $sorted = $values | Sort-Object
    $index  = [math]::Ceiling($p / 100 * $sorted.Count) - 1
    return $sorted[[math]::Max(0, $index)]
}

Write-Host "Sending $Count transactions to $BaseUrl ..."
for ($i = 1; $i -le $Count; $i++) {
    $body = @{ transactionRef = "L$runId-$($i.ToString('0000'))"; accountNumber = $accounts[$i % $accounts.Count]
               amount = 250.00; channel = "Online Banking"; transactionType = "EFT Payment"
               timestamp = $now.AddSeconds(-$i).ToString("yyyy-MM-ddTHH:mm:ssZ") } | ConvertTo-Json -Compress
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $resp  = Invoke-RestMethod -Uri "$BaseUrl/api/transactions" -Method Post -ContentType "application/json" -Body $body
    $watch.Stop()
    $rows.Add([pscustomobject]@{ Ref = $resp.transactionRef; Decision = $resp.decision
                                  ServiceMs = [int]$resp.processingMs; RoundTripMs = [int]$watch.ElapsedMilliseconds })
}

$service = [double[]]($rows | ForEach-Object ServiceMs)
$trip    = [double[]]($rows | ForEach-Object RoundTripMs)
$dir = Join-Path $PSScriptRoot "results"; New-Item -ItemType Directory -Force -Path $dir | Out-Null
$file = Join-Path $dir "latency-$runId.csv"; $rows | Export-Csv -NoTypeInformation -Path $file

$p95 = Pct $service 95
Write-Host ""
Write-Host ("Transactions sent      : {0}" -f $Count)
Write-Host ("Service time  p50 / p95 / max : {0} / {1} / {2} ms" -f (Pct $service 50), $p95, ($service | Measure-Object -Maximum).Maximum)
Write-Host ("Round trip    p50 / p95 / max : {0} / {1} / {2} ms" -f (Pct $trip 50), (Pct $trip 95), ($trip | Measure-Object -Maximum).Maximum)
Write-Host ("NFR-01 (service p95 <= {0} ms): {1}" -f $TargetP95Ms, $(if ($p95 -le $TargetP95Ms) { "PASS" } else { "FAIL" })) `
    -ForegroundColor $(if ($p95 -le $TargetP95Ms) { "Green" } else { "Red" })
Write-Host "Results saved to $file"
