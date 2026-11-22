# Changelog

All notable changes to Combat Auras. Dates are UTC.

## Unreleased

### Fixed
- The spec orb glow and the debug spell grid no longer disappear when the shared "Spells" master switch is off.
- Charge's god-cape check now applies on the same tick (it lagged one tick), and resets on logout / hop.
- The screen glow no longer freezes at its last strength when only fully transparent colours are active.
- The Unpotted notification now respects the Unpotted section's Enable switch.
- Logging in during an imbued heart cooldown no longer assumes the saturated heart's length.
- Spellbook widgets whose name had not loaded yet are no longer remembered as "not a spell".

### Performance
- Blended glow colours no longer change every frame while low HP pulses, so the cached gradient paints stay valid
  instead of being rebuilt each frame.
- Image caches evict the least recently used entry instead of clearing everything when full.
- Applying a preset rebuilds settings once instead of once per key.

### Internal
- Renamed the Java package to `com.combatauras`, the config group to `combatauras` and the main classes to
  `CombatAurasPlugin` / `CombatAurasConfig`, before the first release so no saved settings are affected.
- Settings snapshot arrays are private behind read-only accessors.
- Swing-thread events (config and plugin changes) hand cache and tracker work to the client thread.
- Hit feedback, equipment checks and presets moved out of the plugin class into their own modules.
- CI builds the jar as well as running tests, with pinned actions, a read-only token and a timeout.

## 1.0.0 - 2026-11-21
- First Plugin Hub submission as Combat Auras (formerly Immersive Combat Overlays).
