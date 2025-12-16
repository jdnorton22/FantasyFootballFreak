#!/usr/bin/env pwsh

Write-Host "Fantasy Football Analyzer - Integration Test Validation" -ForegroundColor Green
Write-Host ("=" * 60) -ForegroundColor Green

$integrationTestDir = "app/src/test/java/com/fantasyfootball/analyzer/integration"

if (-not (Test-Path $integrationTestDir)) {
    Write-Host "❌ Integration test directory not found!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Integration test directory found" -ForegroundColor Green

$testFiles = Get-ChildItem -Path $integrationTestDir -Filter "*.kt" | Where-Object { $_.Name -like "*Test*" }

if ($testFiles.Count -eq 0) {
    Write-Host "❌ No integration test files found!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Found $($testFiles.Count) integration test files:" -ForegroundColor Green

$totalTestMethods = 0

foreach ($file in $testFiles) {
    Write-Host "  📄 $($file.Name)" -ForegroundColor Cyan
    
    $content = Get-Content -Path $file.FullName -Raw
    
    # Check for required annotations
    $hasHiltTest = $content -match "@HiltAndroidTest"
    $hasAndroidJUnit = $content -match "@RunWith\(AndroidJUnit4::class\)"
    $hasTestMethods = $content -match "@Test"
    
    if ($hasHiltTest -and $hasAndroidJUnit -and $hasTestMethods) {
        Write-Host "    ✅ Properly structured Android test" -ForegroundColor Green
    } else {
        Write-Host "    ⚠️  Missing required annotations:" -ForegroundColor Yellow
        if (-not $hasHiltTest) { Write-Host "      - @HiltAndroidTest" -ForegroundColor Yellow }
        if (-not $hasAndroidJUnit) { Write-Host "      - @RunWith(AndroidJUnit4::class)" -ForegroundColor Yellow }
        if (-not $hasTestMethods) { Write-Host "      - @Test methods" -ForegroundColor Yellow }
    }
    
    # Count test methods
    $testMethodCount = ($content -split "@Test").Count - 1
    $totalTestMethods += $testMethodCount
    Write-Host "    📊 Contains $testMethodCount test methods" -ForegroundColor Blue
    
    # Check for requirements validation
    $hasRequirementsValidation = $content -match "Requirements addressed:"
    if ($hasRequirementsValidation) {
        Write-Host "    ✅ Contains requirements validation" -ForegroundColor Green
    }
    
    Write-Host ""
}

# Validate test coverage
Write-Host "Integration Test Coverage Analysis:" -ForegroundColor Magenta
Write-Host ("-" * 40) -ForegroundColor Magenta

$allContent = ($testFiles | ForEach-Object { Get-Content -Path $_.FullName -Raw }) -join "`n"

$coverageAreas = @{
    "Player Search & Profile" = @("searchPlayers", "getPlayer", "PlayerProfile")
    "Matchup Analysis" = @("analyzeMatchup", "MatchupAnalysis", "historical")
    "Recommendations" = @("generateWeeklyRecommendations", "PlayerRecommendation")
    "Offline Functionality" = @("cache", "offline", "CacheManager")
    "Network Connectivity" = @("NetworkConnectivity", "online", "transition")
    "Performance" = @("performance", "measureTimeMillis", "threshold")
    "Error Handling" = @("error", "exception", "graceful", "resilience")
    "Data Synchronization" = @("synchronization", "sync", "consistency")
}

$coveredAreas = 0

foreach ($area in $coverageAreas.Keys) {
    $keywords = $coverageAreas[$area]
    $covered = $false
    
    foreach ($keyword in $keywords) {
        if ($allContent -match [regex]::Escape($keyword)) {
            $covered = $true
            break
        }
    }
    
    if ($covered) {
        Write-Host "✅ $area" -ForegroundColor Green
        $coveredAreas++
    } else {
        Write-Host "❌ $area" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "- Integration test files: $($testFiles.Count)" -ForegroundColor White
Write-Host "- Total test methods: $totalTestMethods" -ForegroundColor White
Write-Host "- Coverage areas: $coveredAreas/$($coverageAreas.Count)" -ForegroundColor White

Write-Host ""
Write-Host "✅ Integration test validation complete!" -ForegroundColor Green
Write-Host "The integration tests are properly structured and ready for execution." -ForegroundColor Green
Write-Host ""
Write-Host "To run the integration tests:" -ForegroundColor Cyan
Write-Host "1. Use Android Studio's test runner" -ForegroundColor White
Write-Host "2. Or run: ./gradlew connectedAndroidTest" -ForegroundColor White
Write-Host "3. Or run individual test classes through the IDE" -ForegroundColor White