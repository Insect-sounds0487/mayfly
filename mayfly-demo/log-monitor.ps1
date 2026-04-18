# Mayfly Log Monitor Script
# Usage: .\log-monitor.ps1

$logFile = "mayfly.log"
$checkInterval = 3

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Mayfly Log Monitor" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Log File: $logFile" -ForegroundColor Yellow
Write-Host "Interval: ${checkInterval}s" -ForegroundColor Yellow
Write-Host ""
Write-Host "Start monitoring..." -ForegroundColor Green
Write-Host ""

$lastPosition = 0
$errorCount = 0
$warnCount = 0
$successCount = 0
$failoverCount = 0
$circuitBreakerCount = 0
$timeoutCount = 0

if (Test-Path $logFile) {
    $lastPosition = (Get-Item $logFile).Length
}

while ($true) {
    try {
        if (-not (Test-Path $logFile)) {
            Start-Sleep -Seconds $checkInterval
            continue
        }

        $file = Get-Item $logFile
        $currentSize = $file.Length

        if ($currentSize -gt $lastPosition) {
            $content = Get-Content $logFile -Tail 50

            $newErrors = ($content | Select-String "ERROR").Count
            $newWarns = ($content | Select-String "WARN").Count
            $newSuccess = ($content | Select-String "responded in").Count
            $newFailover = ($content | Select-String "failover").Count
            $newCircuitBreaker = ($content | Select-String "circuit").Count
            $newTimeout = ($content | Select-String -CaseSensitive:$false "timeout").Count

            if ($newErrors -gt 0) {
                $errorCount += $newErrors
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] [ERROR] Found $newErrors error logs (Total: $errorCount)" -ForegroundColor Red
                $errorLines = $content | Select-String "ERROR" | Select-Object -Last 1
                Write-Host "  -> $errorLines" -ForegroundColor DarkRed
            }

            if ($newWarns -gt 0) {
                $warnCount += $newWarns
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] [WARN] Found $newWarns warn logs (Total: $warnCount)" -ForegroundColor Yellow
            }

            if ($newSuccess -gt 0) {
                $successCount += $newSuccess
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] [OK] Success response $newSuccess times (Total: $successCount)" -ForegroundColor Green
            }

            if ($newFailover -gt 0) {
                $failoverCount += $newFailover
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] [FAILOVER] Triggered $newFailover times (Total: $failoverCount)" -ForegroundColor Magenta
            }

            if ($newCircuitBreaker -gt 0) {
                $circuitBreakerCount += $newCircuitBreaker
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] [CIRCUIT] Triggered $newCircuitBreaker times (Total: $circuitBreakerCount)" -ForegroundColor DarkYellow
            }

            if ($newTimeout -gt 0) {
                $timeoutCount += $newTimeout
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] [TIMEOUT] Occurred $newTimeout times (Total: $timeoutCount)" -ForegroundColor DarkCyan
            }

            $lastPosition = $currentSize
        }

        if ((Get-Date).Second % 30 -lt 3) {
            Write-Host ""
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host "  Log Summary ($(Get-Date -Format 'HH:mm:ss'))" -ForegroundColor Cyan
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host "  Success: $successCount" -ForegroundColor Green
            Write-Host "  Errors: $errorCount" -ForegroundColor Red
            Write-Host "  Warnings: $warnCount" -ForegroundColor Yellow
            Write-Host "  Failover: $failoverCount" -ForegroundColor Magenta
            Write-Host "  Circuit Breaker: $circuitBreakerCount" -ForegroundColor DarkYellow
            Write-Host "  Timeout: $timeoutCount" -ForegroundColor DarkCyan
            Write-Host "  Log Size: $([math]::Round($currentSize / 1KB, 2)) KB" -ForegroundColor White
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host ""
        }

        Start-Sleep -Seconds $checkInterval
    }
    catch {
        $errorMsg = $_.Exception.Message
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Monitor error: $errorMsg" -ForegroundColor Red
        Start-Sleep -Seconds 5
    }
}
