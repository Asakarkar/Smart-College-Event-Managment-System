$ErrorActionPreference = 'Stop'

Write-Host "--- 1. Testing GET / (Frontend SPA index.html) ---"
$html = Invoke-WebRequest -Uri 'http://localhost:8085/' -UseBasicParsing
Write-Host "Status Code:" $html.StatusCode "Content length:" $html.Content.Length "bytes"

Write-Host "`n--- 2. Testing Prototype Pattern (POST /api/events/1/clone) ---"
$clone = Invoke-RestMethod -Uri 'http://localhost:8085/api/events/1/clone' -Method Post -Body '{"customName":"[Draft] Hackathon Beta"}' -ContentType 'application/json'
Write-Host "Cloned ID:" $clone.event.id "Name:" $clone.event.name "Status:" $clone.event.status

Write-Host "`n--- 3. Testing Template Method Pattern (POST /api/events/1/workflow) ---"
$wf = Invoke-RestMethod -Uri 'http://localhost:8085/api/events/1/workflow' -Method Post
Write-Host "Workflow:" $wf.report.workflowName
Write-Host "Completed:" $wf.report.completed "Total Steps:" $wf.report.steps.Count
foreach ($step in $wf.report.steps) {
    Write-Host "  Step:" $step.stepName "->" $step.detail
}

Write-Host "`n--- 4. Testing Strategy Pattern (POST /api/fee/calculate) ---"
$fee = Invoke-RestMethod -Uri 'http://localhost:8085/api/fee/calculate' -Method Post -Body '{"category":"TECHNICAL","tier":"EARLY_BIRD"}' -ContentType 'application/json'
Write-Host "Strategy:" $fee.appliedBreakdown.strategyName
Write-Host "Base Price: $" $fee.appliedBreakdown.basePrice "Final Fee: $" $fee.appliedBreakdown.finalFee "(Discount: $($fee.appliedBreakdown.discountPercentage)%)"

Write-Host "`n--- 5. Testing Mediator Pattern (POST /api/registrations/mediate) ---"
$med = Invoke-RestMethod -Uri 'http://localhost:8085/api/registrations/mediate' -Method Post -Body '{"studentId":2,"eventId":2,"feeTier":"STANDARD","notificationChannel":"SMS"}' -ContentType 'application/json'
Write-Host "Mediator Success:" $med.success "Status:" $med.status
Write-Host "Coordination Steps:"
foreach ($line in $med.auditTrail) {
    Write-Host "  $line"
}

Write-Host "`n--- 6. Testing Memento Pattern (GET /api/events/1/memento/history) ---"
$history = Invoke-RestMethod -Uri 'http://localhost:8085/api/events/1/memento/history'
Write-Host "Recorded Checkpoints:" $history.Count
foreach ($m in $history) {
    Write-Host "  v$($m.version) [$($m.timestamp)]: $($m.changeSummary) (Venue: $($m.venue), Cap: $($m.capacity))"
}

Write-Host "`n--- 7. Testing MVC Model (GET /api/events/1) ---"
$mvc = Invoke-RestMethod -Uri 'http://localhost:8085/api/events/1'
Write-Host "Model ID:" $mvc.id "Available Seats:" $mvc.availableSeats "/" $mvc.capacity "Status:" $mvc.status

Write-Host "`n================ ALL 7 ENDPOINTS VERIFIED SUCCESSFULLY ================"
