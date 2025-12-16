# Amenities & Tax Screen Analysis - Android vs iOS

## 🔍 **AMENITIES POPULATION**

### **iOS Implementation** ✅

**Source:** Amenities come from `VehicleInfoResponse` API call

**Flow:**
1. `fetchVehicleInfo()` calls: `GET /api/affiliate/get-vehicle-info/{vehicleId}`
2. Response contains: `VehicleInfoResponse.data.amenities` (Map<String, VehicleAmenity>)
3. Each amenity has: `name`, `label`, `price` (optional)
4. UI displays amenities from: `viewModel.vehicleInfo?.data?.amenities`

**Code Location:**
- ViewModel: `VehicleRatesViewModel.swift` lines 32-74
- View: `VehicleRatesView.swift` lines 1081-1102

```swift
// iOS - Amenities come from VehicleInfo API
if let amenities = viewModel.vehicleInfo?.data?.amenities, !amenities.isEmpty {
    ForEach(Array(amenities.values), id: \.label) { amenity in
        CurrencyTextField(
            label: amenity.name,
            text: Binding(
                get: { amenityPrices[amenity.label] ?? amenity.price ?? "0" },
                set: { amenityPrices[amenity.label] = $0 }
            )
        )
    }
}
```

### **Android Implementation** ⚠️ **ISSUE FOUND**

**Current Source:** Amenities only come from `VehicleRateSettingsStepPrefillData.amenitiesRates`

**Problem:**
- Android fetches `vehicleInfoResult` (line 178-180) but **doesn't use its amenities**
- Only populates amenities from prefill: `prefill.amenitiesRates` (line 216)
- If prefill has no amenities, screen shows "No amenity metadata found"

**Code Location:**
- ViewModel: `VehicleRatesViewModel.kt` lines 178-180, 213-262
- View: `VehicleRatesScreen.kt` lines 361-378

```kotlin
// Android - Currently only uses prefill amenities
if (state.amenityRates.isEmpty()) {
    Text("No amenity metadata found.")
} else {
    state.amenityRates.values.forEach { amenity ->
        MoneyRow(amenity.name, price) { ... }
    }
}
```

**What's Missing:**
- Need to populate `amenityRates` from `vehicleInfoResult.amenities` (metadata)
- Then merge prices from `prefill.amenitiesRates` (if available)

---

## 🎨 **TAX SCREEN DESIGN**

### **iOS Tax Design** ✅

**Layout:** Horizontal layout with toggle on the right side

**Structure:**
- Text field takes most width
- Toggle button positioned to the right with vertical alignment
- Uses `HStack` with `spacing: 12`
- Toggle uses custom `taxToggle()` function

**Tax Fields with Toggle:**
1. CITY TAX
2. STATE TAX  
3. VAT
4. WORKMAN'S COMP
5. OTHER TRANSPORTATION TAX

**Tax Fields WITHOUT Toggle (side-by-side):**
- AIRPORT ARRIVAL TAX + AIRPORT DEP. TAX (in same row)
- SEA PORT TAX + CITY CONGESTION TAX (in same row)

**Code:**
```swift
// iOS Tax Design
HStack(spacing: 12) {
    CurrencyTextField(label: "CITY TAX", ...)
    VStack() {
        Spacer().frame(height: 15)
        taxToggle(isFlat: $cityTaxIsFlat)
    }
}
```

**Toggle Design:**
- Two buttons: "FLAT ($)" and "PERCENT"
- Orange background when selected
- White text when selected, gray when not
- Rounded corners with specific corner radius

### **Android Tax Design** ✅

**Layout:** Horizontal layout with toggle on the right side (matches iOS)

**Structure:**
- Text field uses `Modifier.weight(1f)` to take available space
- Toggle positioned to the right with `Spacer` between
- Uses `Row` with `verticalAlignment = Alignment.CenterVertically`

**Tax Fields with Toggle:**
1. CITY TAX
2. STATE TAX
3. VAT
4. WORKMAN'S COMP
5. OTHER TRANSPORTATION TAX

**Tax Fields WITHOUT Toggle (full width):**
- AIRPORT ARRIVAL TAX
- AIRPORT DEP. TAX
- SEA PORT TAX
- CITY CONGESTION TAX

**Code:**
```kotlin
// Android Tax Design
Row(verticalAlignment = Alignment.CenterVertically) {
    CommonTextField(
        label = label,
        modifier = Modifier.weight(1f),
        ...
    )
    Spacer(modifier = Modifier.width(8.dp))
    CustomSegmentedControl(
        items = listOf("FLAT ($)", "PERCENT"),
        selectedIndex = if (isFlat) 0 else 1,
        ...
    )
}
```

**Toggle Design:**
- Custom segmented control with rounded background
- Orange background when selected
- White text when selected, gray when not
- Similar to iOS design

---

## 📊 **COMPARISON SUMMARY**

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| **Amenities Source** | VehicleInfo API | Prefill only | ❌ Android missing VehicleInfo amenities |
| **Amenities Display** | From `vehicleInfo.data.amenities` | From `state.amenityRates` | ⚠️ Different sources |
| **Tax Layout** | Horizontal with toggle | Horizontal with toggle | ✅ Matches |
| **Tax Toggle Design** | Custom toggle buttons | Custom segmented control | ✅ Similar |
| **Tax Fields Layout** | Some side-by-side | All full width | ⚠️ Minor difference |

---

## 🔧 **RECOMMENDED FIX FOR ANDROID**

**Update `VehicleRatesViewModel.kt` `loadPrefill()` method:**

```kotlin
// Step 4: Fetch vehicle info and rate settings in parallel
val vehicleInfoResult = vehicleIdString?.let { id ->
    repo.getVehicleInfo(id).getOrNull()?.data?.data
}

val rateStep = repo.getVehicleRateSettingsStep().getOrNull()?.data?.data

// Step 5: Update UI state with vehicle info AND amenities
_uiState.update { state ->
    // Get amenities from VehicleInfo (metadata)
    val amenityMetadata = vehicleInfoResult?.amenities ?: emptyMap()
    
    // Convert VehicleAmenity to VehicleAmenityPayload for state
    val amenityRatesMap = amenityMetadata.mapValues { (_, amenity) ->
        VehicleAmenityPayload(
            name = amenity.name ?: "",
            label = amenity.label ?: amenity.id ?: "",
            price = amenity.price?.toDoubleOrNull() ?: 0.0
        )
    }
    
    // Merge with prefill prices if available
    val prefillAmenities = rateStep?.amenitiesRates ?: emptyMap()
    val mergedAmenityPrices = mutableMapOf<String, String>()
    
    amenityRatesMap.forEach { (id, amenity) ->
        val label = amenity.label
        // Use prefill price if available, otherwise use metadata price
        val price = prefillAmenities[label]?.price?.toString() 
            ?: amenity.price.toString()
        mergedAmenityPrices[label] = price
    }
    
    state.copy(
        isLoading = false,
        isPrefilling = true,
        vehicleId = vehicleId ?: rateStep?.vehicleId ?: state.vehicleId,
        vehicleName = vehicleInfoResult?.vehicleType ?: state.vehicleName,
        vehicleTags = listOfNotNull(
            vehicleInfoResult?.vehicleColor,
            vehicleInfoResult?.vehicleYear,
            vehicleInfoResult?.vehicleMake,
            vehicleInfoResult?.vehicleModel
        ),
        vehicleImageUrl = vehicleInfoResult?.vehicleImage ?: state.vehicleImageUrl,
        amenityRates = amenityRatesMap, // Use metadata from VehicleInfo
        amenityPrices = mergedAmenityPrices // Use merged prices
    )
}
```

This ensures Android matches iOS behavior by:
1. ✅ Getting amenity metadata from VehicleInfo API
2. ✅ Merging prices from prefill when available
3. ✅ Displaying all amenities even if prefill is empty




