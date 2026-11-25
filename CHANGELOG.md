# Changelog

All notable changes to Combat Auras. Dates are UTC.

## Unreleased

### Added
- **Visual styles**: four new *Visual Style 1-4* sections, each with its own glow edges, intensity, edge depth,
  tint, motion and shower movement (start edge, travel distance, speed, opacity). Every reminder section has a
  *Visual style* setting (default: the shared look), so for example Low HP can glow on every edge while Thrall
  glows only at the top and bottom. When several styles show at once, each edge is drawn once by the strongest
  style using it, so mixing styles costs no extra drawing. Icon count and size stay shared.
- Defaults: Style 1 is full-screen and urgent, Style 2 top-and-bottom and subtle, Style 3 left-and-right with
  icons sweeping in from the left, Style 4 starts like the shared settings.
- Chambers of Xeric elder, twisted and kodai potions (all three strengths) as boost potions.
- Thrall reminders need the Book of the Dead in the inventory or worn.
- *Hunter meat heal ring* (Food): the delayed second heal shows as a small ring in the item's corner.
- **Item & spellbook glow strength** (General, 25 to 200%, default 100%): brightens or dims glows on inventory
  items, spell icons, the Magic tab and the spec orb. Above 100% layers the glow, and thickens box-style borders.
- **Icon opacity** for showering icons (Icon Shower, default 66%).
- **Pulse / Steady** glow animation for every icon aura that lacked one: spell icons and the Magic tab
  (Shared Combat Spells), the spec orb (Special Attack Ready) and prayer restores (Low Prayer). Inventory ready
  highlights keep their Single flash / Pulse / Solid choice.

- **Heartbeat pulse** on every lasting screen glow, not only Low Health: each spell, warning and reminder section
  has the same *Heartbeat pulse* slider (off by default; Low Health keeps 60%). With it off, spells and reminders keep
  their gentle breathing.
- **One-tick fade-in**: screen glows, icon showers and every icon glow (inventory ready highlights, prayer restores,
  spell icons, the Magic tab, the spec orb) now ease in over 600 ms instead of appearing at full strength. Hit
  flashes (damage, poison and venom ticks) stay instant.

### Changed
- Antipoison and anti-venom expiry reminders moved from Potion Reminders into the Poison and Venom sections
  (*Remind when immunity ends*, *Immunity: warn early*). They now use that section's colour, glow, shower and
  reminder duration. Your antipoison setting carries over; anti-venom has its own toggle.
- While a divine potion runs, regular potions for the same stat show the divine's remaining time (the stat drops
  straight back to base when it ends) instead of a decay estimate.
- When a divine potion ends, only its reminder shows, not the regular potion's as well. Unpotted waits while a
  re-pot reminder for that stat is on screen and only fires if you keep fighting unboosted after it.
- Re-dose protection allows a left-click drink inside the item's *Warn early* window, so you can pre-pot.
- *Combat break re-remind* defaults to 30 s (was 120 s), so ready-spell reminders re-arm between kills and rooms.
- Ice Barrage freezes last 32 ticks (was 33). Ice freezes last 10% longer when the caster wields an Ice ancient
  sceptre.
- Sanfew serum counts as an antipoison.
- The Hunter meat eat lock is 3 ticks like other food; its 7-tick heal no longer looks like a lock or flashes ready.
- Low Health's *Heartbeat pulse* moved next to the other per-section options (same setting, same value).
- *Motion strength* now drives every animation, not only the screen glow: icon pulses, breathing and sweeping
  borders, the inventory Pulse ready animation and shower sway. At 0% they hold steady at full strength.
- The low-prayer restore highlight follows the inventory *Ready border* style and the new glow strength
  (Steady outline when the border is None).

### Removed
- The thrall *CA duration* option: the Master combat achievement tier is always read from the game.
- The Nightmare / Phosani's Nightmare parasite warning and its settings.

### Fixed
- **Thralls, Mark of Darkness, Ward of Arceuus and Shadow Veil** start their real durations again. Since the game's
  August 2026 update these messages begin with a highlight code (`@mes_hl_mag@`) that broke matching.
- **Thrall warning repeating every 10 seconds**: when a thrall outlasted the plugin's estimate, the reminder and
  notification re-armed every 17 ticks. A cast message that arrives a tick before its game variable no longer
  cancels the timer, and the end of a spell no longer starts a second reminder after a warn-early one.
- **Spellbook icons** (thrall and every other tracked spell) get their timer, fill and glow again. The spells sit in
  a nested part of the spellbook, so they are now looked up directly.
- Recast and re-dose protection also work on menu options that carry colour tags.
- The saturated heart is read from the game, so it is right after logging in mid-cooldown or without the click.
- The spec orb glow and the debug spell grid no longer disappear when the shared "Spells" master switch is off.
- Charge's god-cape check now applies on the same tick (it lagged one tick), and resets on logout / hop.
- The screen glow no longer freezes at its last strength when only fully transparent colours are active.
- The Unpotted notification now respects the Unpotted section's Enable switch.
- Logging in during an imbued heart cooldown no longer assumes the saturated heart's length.

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
