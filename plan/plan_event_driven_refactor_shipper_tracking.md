# Plan: Event-Driven Architecture Refactor for ShipperTrackingService

## Status

- Created on 2026-05-03.
- This file is the running log for this task.
- From this prompt onward, all implementation changes for this task must be recorded here.

## Goal

Eliminate polling-based updates by implementing an event-driven model. Data updates will only trigger notifications on actual changes (location updates, batch assignments), replacing the current 2-second polling loop with targeted event emissions.

## Steps

1. **Create `DataChangeEvent` class** to encapsulate event metadata: `type` (`SHIPPER_LOCATION_UPDATED`, `BATCH_UPDATED`, etc.), `sourceId` (shipperId/batchId), and `data` (changed object). Include event type constants for maintainability.

2. **Update `DataChangeListener` interface** in `util/` to replace `onDataChanged()` with `onDataChanged(DataChangeEvent event)`. Provide backward-compatible adapter if needed.

3. **Refactor `ShipperTrackingService.start()`** to remove polling thread loop entirely; convert to logging-only method or light initialization. Verify no other code starts polling threads for this service.

4. **Update `updateShipperLocation()` method** (lines 48-51) to emit `SHIPPER_LOCATION_UPDATED` event with coordinates; call new `notifyListeners(event)` instead of parameterless method.

5. **Find and update batch-related mutations** (assign, update status) across `BatchService.java` and `repository/` to emit `BATCH_UPDATED` events through `ShipperTrackingService`.

6. **Update all UI listeners** (for example `BatchCreationPanel.java`, map panels) to implement new `onDataChanged(event)` signature and respond only to relevant event types (`Platform.runLater()` for JavaFX updates).

7. **Thread-safety audit**: Verify `listeners` `CopyOnWriteArrayList` remains safe; check event emission doesn't occur from UI thread blocking background updates.

## Further Considerations

1. **Polling elsewhere?** Search codebase for other `Thread.sleep()` or `Timer` calls in polling loops. Do not allow duplicated polling behavior in other services.

2. **Backward compatibility strategy**: If old code still calls `ShipperTrackingService.refreshData()`, decide whether it should emit a generic `DATA_CHANGED` event or use an adapter wrapper.

3. **Event persistence/replay**: Prefer fire-only-to-current-listeners for now unless debugging needs prove otherwise.

4. **`getAllBatches()` optimization**: Keep current behavior to avoid breaking callers, but discourage repeated external polling usage.

5. **Testing strategy**: Add focused tests that verify events fire for location and batch updates. Prefer JUnit 5 with listener mocks and event argument verification.

## Change Log

- 2026-05-03: Created initial task plan from user prompt.
- 2026-05-03: Added `DataChangeEvent` with event types `DATA_CHANGED`, `SHIPPER_LOCATION_UPDATED`, and `BATCH_UPDATED`.
- 2026-05-03: Updated `DataChangeListener` to receive `DataChangeEvent`.
- 2026-05-03: Removed polling loop from `ShipperTrackingService.start()` and switched listener notification to explicit event emission.
- 2026-05-03: Updated shipper location updates to emit `SHIPPER_LOCATION_UPDATED`.
- 2026-05-03: Updated batch creation and batch assignment flows to emit `BATCH_UPDATED`.
- 2026-05-03: Updated admin and shipper UI listeners to the new event signature and filtered admin refresh logic by event type.
- 2026-05-03: Noted remaining architectural gap: `shipper/ShipperApp.java` still polls for assigned batches in a separate JVM, so admin in-memory events do not cover that process boundary.
- 2026-05-03: Verified updated files compile with `javac` against `target/classes` and local JavaFX jars; only existing `jakarta.persistence` classpath warnings remained.
