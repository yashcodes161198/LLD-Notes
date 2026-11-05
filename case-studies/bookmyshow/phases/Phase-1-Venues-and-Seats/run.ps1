$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force -Path out | Out-Null
    $sources = @(Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & javac --release 17 -encoding UTF-8 -d out @sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    & java -cp out bookmyshow.Main
    if ($LASTEXITCODE -ne 0) { throw 'Run failed' }
} finally { Pop-Location }
