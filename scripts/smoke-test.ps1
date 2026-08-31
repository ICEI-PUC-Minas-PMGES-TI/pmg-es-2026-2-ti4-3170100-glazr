param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$MailpitUrl = "http://localhost:8025",
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

function Wait-Mail {
    param([string]$Recipient, [string]$SubjectPattern)
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        $listing = Invoke-RestMethod -Method Get -Uri "$MailpitUrl/api/v1/messages"
        $summary = $listing.messages | Where-Object {
            $_.To[0].Address -eq $Recipient -and $_.Subject -like $SubjectPattern
        } | Select-Object -First 1
        if ($null -ne $summary) {
            return Invoke-RestMethod -Method Get -Uri "$MailpitUrl/api/v1/message/$($summary.ID)"
        }
        Start-Sleep -Seconds 1
    }
    throw "O e-mail esperado não chegou ao Mailpit."
}

function Get-FragmentToken {
    param([string]$Text)
    $match = [regex]::Match($Text, '#token=([A-Za-z0-9_-]+)')
    if (-not $match.Success) { throw "A mensagem não contém um token temporário válido." }
    return $match.Groups[1].Value
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

$userEmail = "smoke-$suffix@example.invalid"
$userBody = @{
    email = $userEmail
    displayName = "Usuário Smoke"
    accessProfileIds = @($profile.id)
} | ConvertTo-Json
$createdUser = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/users" -WebSession $adminSession `
    -Headers $adminHeaders -ContentType "application/json" -Body $userBody

$invitationMail = Wait-Mail -Recipient $userEmail -SubjectPattern "*convite*"
$invitationToken = Get-FragmentToken -Text $invitationMail.Text
$temporaryPassword = "Smoke-$([Guid]::NewGuid())"

$operatorSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$operatorHeaders = Get-CsrfHeader -Session $operatorSession
$activation = @{ token = $invitationToken; password = $temporaryPassword } | ConvertTo-Json
$activatedUser = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/invitations/accept-with-password" `
    -WebSession $operatorSession -Headers $operatorHeaders -ContentType "application/json" -Body $activation
if ($activatedUser.id -ne $createdUser.id) { throw "A ativação não preservou o UUID do usuário local." }

try {
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/permissions" -WebSession $operatorSession | Out-Null
    throw "O usuário sem PERMISSION_READ acessou o catálogo indevidamente."
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw }
}

$changedPassword = "Smoke-Changed-$([Guid]::NewGuid())"
$operatorHeaders = Get-CsrfHeader -Session $operatorSession
$changeBody = @{ currentPassword = $temporaryPassword; newPassword = $changedPassword } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/password/change" -WebSession $operatorSession `
    -Headers $operatorHeaders -ContentType "application/json" -Body $changeBody | Out-Null

$resetSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$resetHeaders = Get-CsrfHeader -Session $resetSession
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/password-reset/request" -WebSession $resetSession `
    -Headers $resetHeaders -ContentType "application/json" -Body (@{ email = $userEmail } | ConvertTo-Json) | Out-Null
$resetMail = Wait-Mail -Recipient $userEmail -SubjectPattern "*Redefini*"
$resetToken = Get-FragmentToken -Text $resetMail.Text
$finalPassword = "Smoke-Final-$([Guid]::NewGuid())"
$resetHeaders = Get-CsrfHeader -Session $resetSession
$resetBody = @{ token = $resetToken; newPassword = $finalPassword } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/password-reset/complete" -WebSession $resetSession `
    -Headers $resetHeaders -ContentType "application/json" -Body $resetBody | Out-Null

try {
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/auth/me" -WebSession $operatorSession | Out-Null
    throw "A sessão continuou válida depois da redefinição de senha."
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw }
}

$finalSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$finalHeaders = Get-CsrfHeader -Session $finalSession
$finalLogin = @{ email = $userEmail; password = $finalPassword } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -WebSession $finalSession `
    -Headers $finalHeaders -ContentType "application/json" -Body $finalLogin | Out-Null
$finalHeaders = Get-CsrfHeader -Session $finalSession
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/logout" -WebSession $finalSession `
    -Headers $finalHeaders | Out-Null

$adminHeaders = Get-CsrfHeader -Session $adminSession
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/logout" -WebSession $adminSession `
    -Headers $adminHeaders | Out-Null

try {
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/auth/me" -WebSession $adminSession | Out-Null
    throw "A sessão continuou válida depois do logout."
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw }
}

Write-Output "Smoke test concluído: login admin, convite SMTP, ativação, UUID, 403, alteração/reset de senha, sessões e logout validados."
