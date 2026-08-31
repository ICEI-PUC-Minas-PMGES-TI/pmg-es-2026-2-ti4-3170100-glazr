param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminEmail = $env:GLAZR_BOOTSTRAP_ADMIN_EMAIL,
    [string]$AdminPassword = $env:GLAZR_BOOTSTRAP_ADMIN_PASSWORD
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AdminEmail) -or [string]::IsNullOrEmpty($AdminPassword)) {
    throw "Defina GLAZR_BOOTSTRAP_ADMIN_EMAIL e GLAZR_BOOTSTRAP_ADMIN_PASSWORD no processo atual."
}

function Get-CsrfHeader {
    param([Microsoft.PowerShell.Commands.WebRequestSession]$Session)
    $csrf = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/auth/csrf" -WebSession $Session
    return @{ $csrf.headerName = $csrf.token }
}

$adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$adminHeaders = Get-CsrfHeader -Session $adminSession
$loginBody = @{ email = $AdminEmail; password = $AdminPassword } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -WebSession $adminSession `
    -Headers $adminHeaders -ContentType "application/json" -Body $loginBody | Out-Null

$me = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/auth/me" -WebSession $adminSession
if ($me.email -ne $AdminEmail.ToLowerInvariant()) { throw "O usuário autenticado não corresponde ao administrador." }

$adminHeaders = Get-CsrfHeader -Session $adminSession
$suffix = [Guid]::NewGuid().ToString("N").Substring(0, 8)
$profileBody = @{
    name = "SMOKE_$suffix"
    description = "Perfil temporário do smoke test"
    active = $true
    permissionKeys = @("USER_READ")
} | ConvertTo-Json
$profile = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/access-profiles" -WebSession $adminSession `
    -Headers $adminHeaders -ContentType "application/json" -Body $profileBody

$temporaryPassword = "Smoke-$([Guid]::NewGuid())"
$userEmail = "smoke-$suffix@example.invalid"
$userBody = @{
    email = $userEmail
    displayName = "Usuário Smoke"
    password = $temporaryPassword
    accessProfileIds = @($profile.id)
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/users" -WebSession $adminSession `
    -Headers $adminHeaders -ContentType "application/json" -Body $userBody | Out-Null

$operatorSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$operatorHeaders = Get-CsrfHeader -Session $operatorSession
$operatorLogin = @{ email = $userEmail; password = $temporaryPassword } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -WebSession $operatorSession `
    -Headers $operatorHeaders -ContentType "application/json" -Body $operatorLogin | Out-Null

try {
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/permissions" -WebSession $operatorSession | Out-Null
    throw "O usuário sem PERMISSION_READ acessou o catálogo indevidamente."
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw }
}

$operatorHeaders = Get-CsrfHeader -Session $operatorSession
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/logout" -WebSession $operatorSession `
    -Headers $operatorHeaders | Out-Null

$adminHeaders = Get-CsrfHeader -Session $adminSession
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/logout" -WebSession $adminSession `
    -Headers $adminHeaders | Out-Null

try {
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/auth/me" -WebSession $adminSession | Out-Null
    throw "A sessão continuou válida depois do logout."
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw }
}

Write-Output "Smoke test concluído: CSRF, login, /me, perfil, usuário, 403 e logout validados."
