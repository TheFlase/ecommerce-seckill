<#
.SYNOPSIS
  Concurrent seckill load test via Gateway.

.EXAMPLE
  .\scripts\load-seckill.ps1
  .\scripts\load-seckill.ps1 -Users 80 -Concurrency 40 -ActivityId 1
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [long]$ActivityId = 1,
    [int]$Users = 50,
    [int]$Concurrency = 20,
    [string]$UserPrefix = "loaduser",
    [switch]$SkipWarmUp
)

$ErrorActionPreference = "Stop"
if ($Users -lt 1) { throw "Users must be >= 1" }
if ($Concurrency -lt 1) { throw "Concurrency must be >= 1" }
if ($Concurrency -gt $Users) { $Concurrency = $Users }

function Invoke-JsonPost {
    param(
        [string]$Url,
        [string]$Body,
        [hashtable]$Headers = @{}
    )
    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($tmp, $Body, [System.Text.UTF8Encoding]::new($false))
        $headerArgs = @()
        foreach ($k in $Headers.Keys) {
            $headerArgs += @("-H", "${k}: $($Headers[$k])")
        }
        $raw = & curl.exe -s -w "`n%{http_code}" -X POST $Url `
            -H "Content-Type: application/json" `
            @headerArgs `
            --data-binary "@$tmp"
        $parts = $raw -split "`n"
        $code = $parts[-1]
        $bodyText = if ($parts.Length -gt 1) { ($parts[0..($parts.Length - 2)] -join "`n") } else { "" }
        return @{ HttpCode = $code; Body = $bodyText }
    }
    finally {
        Remove-Item $tmp -ErrorAction SilentlyContinue
    }
}

Write-Host "=== seckill load test ==="
Write-Host "BaseUrl=$BaseUrl ActivityId=$ActivityId Users=$Users Concurrency=$Concurrency"

# 1) admin warm-up
if (-not $SkipWarmUp) {
    Write-Host "`n[1] admin login + warm-up"
    $admin = Invoke-JsonPost -Url "$BaseUrl/user/login" -Body '{"username":"admin","password":"password"}'
    $adminToken = ($admin.Body | ConvertFrom-Json).data
    if (-not $adminToken) { throw "admin login failed: $($admin.Body)" }
    $wu = & curl.exe -s -w "`n%{http_code}" -X POST "$BaseUrl/seckill/warm-up/$ActivityId" -H "Authorization: $adminToken"
    Write-Host "warm-up => $wu"
}
else {
    Write-Host "`n[1] skip warm-up"
}

# 2) register + login N users (unique suffix so each run is fresh)
$runId = Get-Date -Format "yyyyMMddHHmmss"
Write-Host "`n[2] prepare $Users users (prefix=${UserPrefix}_${runId}_*)"
$tokens = New-Object System.Collections.Generic.List[string]
for ($i = 1; $i -le $Users; $i++) {
    $name = "${UserPrefix}_${runId}_$i"
    $phone = "139{0}{1:D4}" -f $runId.Substring(8, 6), $i
    $regBody = @{
        username = $name
        password = "password"
        phone    = $phone
        nickname = $name
    } | ConvertTo-Json -Compress
    [void](Invoke-JsonPost -Url "$BaseUrl/user/register" -Body $regBody)

    $loginBody = (@{ username = $name; password = "password" } | ConvertTo-Json -Compress)
    $login = Invoke-JsonPost -Url "$BaseUrl/user/login" -Body $loginBody
    $token = $null
    try { $token = ($login.Body | ConvertFrom-Json).data } catch { }
    if (-not $token) {
        throw "login failed for $name : $($login.Body)"
    }
    $tokens.Add([string]$token) | Out-Null
    if ($i % 10 -eq 0 -or $i -eq $Users) {
        Write-Host "  prepared $i / $Users"
    }
}

# 3) concurrent do-seckill
Write-Host "`n[3] fire $Users requests, concurrency=$Concurrency"
$seckillUrl = "$BaseUrl/seckill/do-seckill?activityId=$ActivityId&quantity=1"

$scriptBlock = {
    param($Url, $Token, $Index)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $resp = & curl.exe -s -w "`n%{http_code}" -X POST $Url -H "Authorization: $Token" --max-time 30
        $sw.Stop()
        $parts = $resp -split "`n"
        $http = $parts[-1]
        $body = if ($parts.Length -gt 1) { ($parts[0..($parts.Length - 2)] -join "`n") } else { "" }
        $code = $null
        $msg = $body
        try {
            $j = $body | ConvertFrom-Json
            $code = $j.code
            $msg = [string]$j.message
            if ($j.data) { $msg = "$msg|$($j.data)" }
        } catch { }
        [pscustomobject]@{
            Index    = $Index
            Ok       = ($http -eq "200" -and $code -eq 200)
            HttpCode = $http
            BizCode  = $code
            Message  = $msg
            Ms       = [int]$sw.ElapsedMilliseconds
        }
    }
    catch {
        $sw.Stop()
        [pscustomobject]@{
            Index    = $Index
            Ok       = $false
            HttpCode = "ERR"
            BizCode  = -1
            Message  = $_.Exception.Message
            Ms       = [int]$sw.ElapsedMilliseconds
        }
    }
}

$pool = [runspacefactory]::CreateRunspacePool(1, $Concurrency)
$pool.Open()
$jobs = @()
$wall = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $tokens.Count; $i++) {
    $ps = [powershell]::Create().AddScript($scriptBlock).AddArgument($seckillUrl).AddArgument($tokens[$i]).AddArgument($i)
    $ps.RunspacePool = $pool
    $jobs += [pscustomobject]@{ Pipe = $ps; Handle = $ps.BeginInvoke() }
}
$resultList = New-Object System.Collections.ArrayList
foreach ($j in $jobs) {
    [void]$j.Handle.AsyncWaitHandle.WaitOne()
    $out = $j.Pipe.EndInvoke($j.Handle)
    if ($out) { foreach ($o in @($out)) { [void]$resultList.Add($o) } }
    $j.Pipe.Dispose()
}
$wall.Stop()
$pool.Close()
$pool.Dispose()

# 4) summarize
$rows = @($resultList.ToArray())
$success = @($rows | Where-Object { $_.Ok }).Count
$dup = @($rows | Where-Object { $_.Message -match "已经参与" }).Count
$sold = @($rows | Where-Object { $_.Message -match "抢光" }).Count
$limited = @($rows | Where-Object { $_.Message -match "Sentinel|限流|繁忙" }).Count
$otherFail = $rows.Count - $success - $dup - $sold - $limited
$latencies = @($rows | ForEach-Object { [int]$_.Ms } | Sort-Object)
function Get-Percentile([object[]]$sorted, [double]$p) {
    if (-not $sorted -or $sorted.Count -eq 0) { return 0 }
    $idx = [Math]::Min($sorted.Count - 1, [int][Math]::Ceiling($p * $sorted.Count) - 1)
    if ($idx -lt 0) { $idx = 0 }
    return [int]$sorted[$idx]
}
$p50 = Get-Percentile $latencies 0.50
$p95 = Get-Percentile $latencies 0.95
$p99 = Get-Percentile $latencies 0.99
$avg = if ($latencies.Count) { [int](($latencies | Measure-Object -Average).Average) } else { 0 }
$qps = if ($wall.Elapsed.TotalSeconds -gt 0) { [Math]::Round($rows.Count / $wall.Elapsed.TotalSeconds, 2) } else { 0 }

Write-Host "`n=== summary ==="
Write-Host ("total={0} wall={1:N2}s qps={2}" -f $rows.Count, $wall.Elapsed.TotalSeconds, $qps)
Write-Host "success=$success  duplicate=$dup  sold_out=$sold  rate_limited=$limited  other_fail=$otherFail"
Write-Host "latency_ms avg=$avg p50=$p50 p95=$p95 p99=$p99"

# top failure messages
$failMsgs = $rows | Where-Object { -not $_.Ok } | Group-Object Message | Sort-Object Count -Descending | Select-Object -First 5
if ($failMsgs) {
    Write-Host "`nTop failure messages:"
    foreach ($g in $failMsgs) {
        Write-Host ("  [{0}] {1}" -f $g.Count, $g.Name)
    }
}

Write-Host "`nDONE"
exit 0
