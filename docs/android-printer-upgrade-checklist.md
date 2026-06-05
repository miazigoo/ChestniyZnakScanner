# Android TSD printer upgrade checklist

Source reference: `/home/b00bs/testProgects/ChestniyZnak`.
Current app: `/home/b00bs/my_less/SaaS/ChestniyZnakScanner`.

## Goal
Bring the current TSD app to the newer printer workflow: a supplier user selects one active supplier printer, the selection follows that user across Desktop/TSD, and closing or reprinting a box prints the SSCC label.

## Backend contract to use
- `GET /api/v1/tsd/chestniy-znak/packing/printer/printers`
- `POST /api/v1/tsd/chestniy-znak/packing/printer/printer-selection`
- `POST /api/v1/tsd/chestniy-znak/packing/boxes/{box_id}/print-label`
- `POST /api/v1/tsd/chestniy-znak/packing/boxes/{box_id}/print-result`

`device_id` may still be sent for audit/backward compatibility, but the backend selection key is now the authenticated supplier user, not the device.

## Port from source app
- [x] `feature/printer/PrinterSettingsScreen.kt`
- [x] `feature/printer/PrinterSettingsUiState.kt`
- [x] `feature/printer/PrinterSettingsViewModel.kt`
- [x] `domain/model/ClientPrinter.kt`
- [x] `domain/usecase/GetClientPrinterSelectionUseCase.kt`
- [x] `domain/usecase/SetClientPrinterSelectionUseCase.kt`
- [x] `domain/usecase/PrintPackingBoxLabelUseCase.kt`
- [x] printer DTO mappings from `data/remote/dto`
- [x] Retrofit methods from source `PackingApi.kt`: `clientPrinters`, `setClientPrinterSelection`, `printBoxLabel`
- [x] repository methods from source `RemotePackingRepository.kt`
- [x] navigation item `PrinterSettings`
- [x] close-box UI fields `printOk` and `printError` from source packing models/screens

## Adjustments for this SaaS backend
- [x] Map legacy source endpoint `packing/printer/boxes/{boxId}/print` to SaaS endpoints `print-label` and `print-result`.
- [x] Send backend print job to the printer with raw TCP and then call `print-result`.
- [x] If current backend close response does not include print result, call `print-label` after successful close.
- [x] Show printer selection in service/settings menu.
- [x] Show a blocking warning before close/reprint if no printer is selected and more than one active printer exists.
- [x] Auto-select only when the supplier has exactly one active printer.

## Acceptance criteria
- [x] Supplier user sees all active printers created by the supplier organization.
- [x] User selection persists across Desktop and TSD for the same account.
- [x] Another supplier user can select a different printer without changing the first user's selection.
- [x] Closing a box triggers label printing and reports success/failure to backend.
- [x] Reprint from box detail is available.
- [ ] Optional: add quick reprint action directly in the boxes list.
- [x] Android `./gradlew test` passes.

## Camera-only scanner upgrade
- [x] Port optimized CameraX scanner core from `/home/b00bs/testProgects/ChestniyZnak`.
- [x] Add ROI overlay and stable two-frame detection before accepting a scan.
- [x] Add continuous autofocus, tap-to-focus, auto-focus on detected barcode, pinch zoom, double-tap zoom, manual zoom buttons, torch toggle, and camera hints.
- [x] Keep QR token camera flow compatible through `QrCodeCameraPreview`.
- [x] Keep DataMatrix packing/verify flow compatible through `DataMatrixCameraPreview`.
- [x] Add camera mode to box edit/add-code flow.
- [x] Add camera mode to box lookup/SSCC flow.
- [x] Keep TSD and camera input modes isolated so hidden HID scans do not process while a camera-only screen is selected.
- [x] Android `./gradlew test --rerun-tasks` passes.
