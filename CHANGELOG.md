# Changelog

All notable changes to Combat Auras. Dates are UTC.

## Unreleased

### Added
- **Item & spellbook glow strength** (General, 25 to 200%, default 100%): brightens or dims glows on inventory
  items, spell icons, the Magic tab and the spec orb. Above 100% layers the glow, and thickens box-style borders.
- **Icon opacity** for showering icons (Icon Shower, default 66%).
- **Pulse / Steady** glow animation for every icon aura that lacked one: spell icons and the Magic tab
  (Shared Combat Spells), the spec orb (Special Attack Ready) and prayer restores (Low Prayer). Inventory ready
  highlights keep their Single flash / Pulse / Solid choice.

### Changed
- *Motion strength* now drives every animation, not only the screen glow: icon pulses, breathing and sweeping
  borders, the inventory Pulse ready animation and shower sway. At 0% they hold steady at full strength.
- The low-prayer restore highlight follows the inventory *Ready border* style and the new glow strength
  (Steady outline when the border is None).

### Removed
- The Nightmare / Phosani's Nightmare parasite warning and its settings.

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

### Performance
- Image and widget caches are keyed by primitive longs (4-way set-associative, LRU within each set), so a frame's
  lookups no longer box a key per inventory item or spellbook widget (up to ~100 per frame with the spellbook open).
- Item classification is a plain array indexed by item id instead of a boxed `HashMap`.
- Cooldown timer labels are built once per remaining tick and reused, instead of a new string (and a
  `String.format` for the last ten seconds) per icon per frame. This also fixes "5,4"-style labels in locales that
  use a decimal comma.
- The Magic tab glow colour is only rebuilt when the set of ready spells changes.

### Internal
- Inventory, spellbook and damage-flash rendering read the settings snapshot instead of the config each frame.
- Inventory and spellbook icons share one glow, fill and radial routine; animation curves live in `render/Motion`.
- Removed unused code (unknown-duration cooldowns, an unused notification type and helpers) and renamed
  `ImmersiveStatusOverlay` to `StatusOverlay`.
- Renamed the Java package to `com.combatauras`, the config group to `combatauras` and the main classes to
  `CombatAurasPlugin` / `CombatAurasConfig`, before the first release so no saved settings are affected.
- Settings snapshot arrays are private behind read-only accessors.
- Swing-thread events (config and plugin changes) hand cache and tracker work to the client thread.
- Hit feedback, equipment checks and presets moved out of the plugin class into their own modules.
- CI builds the jar as well as running tests, with pinned actions, a read-only token and a timeout.

## 1.0.0 - 2026-11-21
- First Plugin Hub submission as Combat Auras (formerly Immersive Combat Overlays).
