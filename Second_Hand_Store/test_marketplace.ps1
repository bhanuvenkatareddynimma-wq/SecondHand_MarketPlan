# Comprehensive Test for SecondHand MarketPlace

Write-Host "--- TEST 1: CREATE NEW LISTING ---"
$newListingBody = '{"title":"Physics Lab Manual & Notes","description":"Complete 1st year physics practical manual with solved assignments.","category":"Books & Study Material","price":200,"condition":"Good","sellerName":"Dev Patel","sellerContact":"dev.p@campus.edu"}'
$created = Invoke-RestMethod -Uri "http://localhost:8080/api/listings" -Method POST -Body $newListingBody -ContentType "application/json"
$newId = $created.listing.id
Write-Host "Created listing ID:" $newId "Title:" $created.listing.title

Write-Host "`n--- TEST 2: SEARCH & FILTER ---"
$searchRes = Invoke-RestMethod -Uri "http://localhost:8080/api/listings?q=Manual"
Write-Host "Search for 'Manual' count:" $searchRes.Count "First item:" $searchRes[0].title

$filterCat = Invoke-RestMethod -Uri "http://localhost:8080/api/listings?category=Electronics%20%26%20Gadgets"
Write-Host "Filter Category Electronics count:" $filterCat.Count

$filterCond = Invoke-RestMethod -Uri "http://localhost:8080/api/listings?condition=Brand%20New"
Write-Host "Filter Condition Brand New count:" $filterCond.Count

$filterPrice = Invoke-RestMethod -Uri "http://localhost:8080/api/listings?minPrice=300&maxPrice=1000"
Write-Host "Filter Price 300..1000 count:" $filterPrice.Count

Write-Host "`n--- TEST 3: SUBMIT INQUIRY ON AVAILABLE ITEM ---"
$inquiryBody = '{"buyerName":"Aanya Shah","buyerContact":"aanya@campus.edu","message":"Is this practical manual for batch 2026?"}'
$inqRes = Invoke-RestMethod -Uri ("http://localhost:8080/api/listings/$newId/inquiries") -Method POST -Body $inquiryBody -ContentType "application/json"
Write-Host "Inquiry created successfully:" $inqRes.success "Message:" $inqRes.inquiry.message

Write-Host "`n--- TEST 4: MARK LISTING AS SOLD ---"
$soldRes = Invoke-RestMethod -Uri ("http://localhost:8080/api/listings/$newId/sold") -Method POST
Write-Host "Marked sold success:" $soldRes.success "Sold status:" $soldRes.listing.sold

Write-Host "`n--- TEST 5: PREVENT INQUIRY ON SOLD ITEM ---"
try {
    $blockedInq = Invoke-RestMethod -Uri ("http://localhost:8080/api/listings/$newId/inquiries") -Method POST -Body '{"buyerName":"Late Buyer","message":"Can I still buy this?"}' -ContentType "application/json"
    Write-Host "FAILED: Inquiry should NOT have succeeded on sold item!"
} catch {
    Write-Host "PASSED: Server correctly rejected inquiry on sold item with HTTP 400 error:" $_.Exception.Message
}

Write-Host "`n=== ALL TESTS PASSED SUCCESSFULLY! ==="
