<#
.SYNOPSIS
  Exercises the running API against the test cases in the traceability matrix.
.DESCRIPTION
  Start the application first (gradlew bootRun), then run from PowerShell:
      powershell -ExecutionPolicy Bypass -File scripts\api-smoke-test.ps1
  Prints PASS/FAIL per case and saves the results to scripts\results\smoke-<time>.csv.
  Works in Windows PowerShell 5.1 and PowerShell 7.
#>
param([string]$BaseUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"
$runId   = Get-Date -Format "MMddHHmmss"
$counter = 0
# A random day in the past, so repeated runs never share a velocity window.
$base    = (Get-Date).ToUniversalTime().AddDays(-(Get-Random -Minimum 30 -Maximum 3000)).Date.AddHours(9)
$results = New-Object System.Collections.Generic.List[object]

function New-Ref { $script:counter++; return "S$runId-$($script:counter.ToString('00'))" }
function Iso([datetime]$t) { return $t.ToString("yyyy-MM-ddTHH:mm:ssZ") }

function Invoke-Api([string]$Method, [string]$Path, [string]$Body) {
    try {
        $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true }
        if ($Body) { $params.ContentType = "application/json"; $params.Body = $Body }
        $r = Invoke-WebRequest @params
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw "Cannot reach $BaseUrl - is the application running?" }
        $text = $_.ErrorDetails.Message
        if (-not $text) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream()); $text = $reader.ReadToEnd()
        }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $text }
    }
}

function Txn([string]$Ref, [string]$Account, $Amount, [datetime]$When,
             [string]$Channel = "ATM", [string]$Type = "POS Purchase") {
    return (@{ transactionRef = $Ref; accountNumber = $Account; amount = $Amount; channel = $Channel
               transactionType = $Type; timestamp = (Iso $When) } | ConvertTo-Json -Compress)
}

function Check([string]$Tc, [string]$Name, $Response, [int]$ExpectStatus, [string]$ExpectText = "") {
    $ok = ($Response.Status -eq $ExpectStatus) -and ($ExpectText -eq "" -or $Response.Body -like "*$ExpectText*")
    if ($Response.Body -match '"trace"|"exception"|at za\.co\.') { $ok = $false }   # NFR-05: no internals
    $results.Add([pscustomobject]@{ TC = $Tc; Case = $Name; Expected = $ExpectStatus; Actual = $Response.Status
                                    Result = $(if ($ok) { "PASS" } else { "FAIL" }); Body = $Response.Body })
    $colour = if ($ok) { "Green" } else { "Red" }
    Write-Host ("{0,-6} {1,-58} {2} -> {3}  {4}" -f $Tc, $Name, $ExpectStatus, $Response.Status,
               $(if ($ok) { "PASS" } else { "FAIL" })) -ForegroundColor $colour
}

$acct  = "4128728463"
$now   = (Get-Date).ToUniversalTime()
$ref1  = New-Ref

Check "TC-01" "Valid transaction is accepted"            (Invoke-Api POST "/api/transactions" (Txn $ref1 $acct 250.00 $now.AddMinutes(-1))) 201 '"decision"'
Check "TC-06" "Same reference again is rejected"         (Invoke-Api POST "/api/transactions" (Txn $ref1 $acct 250.00 $now.AddMinutes(-1))) 409 "DUPLICATE_TRANSACTION"
Check "TC-02" "Unknown account is rejected"              (Invoke-Api POST "/api/transactions" (Txn (New-Ref) "0000000000" 50 $now)) 400 "INVALID_ACCOUNT"
Check "TC-03" "Amount of zero is rejected"               (Invoke-Api POST "/api/transactions" (Txn (New-Ref) $acct 0 $now)) 400 "VALIDATION_FAILED"
Check "TC-03" "Negative amount is rejected"              (Invoke-Api POST "/api/transactions" (Txn (New-Ref) $acct -10 $now)) 400 "VALIDATION_FAILED"
Check "TC-04" "Unknown channel is rejected"              (Invoke-Api POST "/api/transactions" (Txn (New-Ref) $acct 50 $now "Fax")) 400 "INVALID_CHANNEL"
Check "TC-04" "Unknown transaction type is rejected"     (Invoke-Api POST "/api/transactions" (Txn (New-Ref) $acct 50 $now "ATM" "Barter")) 400 "INVALID_TRANSACTION_TYPE"
$noTime = (@{ transactionRef = (New-Ref); accountNumber = $acct; amount = 50; channel = "ATM"; transactionType = "POS Purchase" } | ConvertTo-Json -Compress)
Check "TC-05" "Missing timestamp is rejected"            (Invoke-Api POST "/api/transactions" $noTime) 400 "VALIDATION_FAILED"
Check "TC-05" "Timestamp 1 hour in the future is rejected" (Invoke-Api POST "/api/transactions" (Txn (New-Ref) $acct 50 $now.AddHours(1))) 400 "INVALID_TIMESTAMP"
Check "TC-24" "Malformed JSON gives {code, message} only" (Invoke-Api POST "/api/transactions" '{"transactionRef": ') 400 "MALFORMED_REQUEST"
Check "TC-22" "SQL injection in account number is harmless" (Invoke-Api POST "/api/transactions" (Txn (New-Ref) "' OR '1'='1" 50 $now)) 400 "INVALID_ACCOUNT"

# Velocity: five R50 transactions on one account, two minutes apart, at a random past time.
$velocityAcct = "4141227216"
$refs = @()
for ($i = 0; $i -lt 5; $i++) {
    $r = New-Ref; $refs += $r
    $resp = Invoke-Api POST "/api/transactions" (Txn $r $velocityAcct 50.00 $base.AddMinutes(2 * $i))
    if ($i -eq 3) { Check "TC-08" "4th micro-transaction in 10 min is approved" $resp 201 '"decision":"Success"' }
    if ($i -eq 4) { Check "TC-09" "5th micro-transaction in 10 min is flagged" $resp 201 '"decision":"Flagged"' }
}
Check "TC-19" "Look-up shows the flagged status and its alert" (Invoke-Api GET "/api/transactions/$($refs[4])") 200 "VELOCITY_MICRO"
Check "TC-20" "Look-up of an unknown reference returns 404"    (Invoke-Api GET "/api/transactions/NO-SUCH-REF") 404 "TRANSACTION_NOT_FOUND"

$dir = Join-Path $PSScriptRoot "results"; New-Item -ItemType Directory -Force -Path $dir | Out-Null
$file = Join-Path $dir "smoke-$runId.csv"; $results | Export-Csv -NoTypeInformation -Path $file
$failed = @($results | Where-Object Result -eq "FAIL").Count
Write-Host ""
Write-Host ("{0} of {1} cases passed. Results saved to {2}" -f ($results.Count - $failed), $results.Count, $file) `
    -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
if ($failed -gt 0) { exit 1 }
