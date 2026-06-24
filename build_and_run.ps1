# Configure environment
. .\setup_env.ps1

# Build and Test
Write-Host "Building and Testing..."
mvn clean install
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed."
    exit $LASTEXITCODE
}

# Run
Write-Host "Starting Application..."
mvn spring-boot:run
