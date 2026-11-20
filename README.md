# Immersive Combat Overlays

A unified RuneLite plugin combining inventory/potion cooldown overlays, procedural health & status screen overlays, and combat spell recast/expiry reminders.

## Features

- **Combat Spell Overlays & Reminders**:
  - Tracks **Resurrect Thrall**, **Death Charge**, **Mark of Darkness**, **Ward of Arceuus**, **Shadow Veil**, **Corruption**, **Vengeance**, **Vengeance Other** and **Charge** until the spell can be recast (the later of the buff ending and the recast cooldown).
  - Uses the same game varbits and durations as RuneLite's Timers & Buffs plugin (thrall duration auto-detects the Master Combat Achievement tier; Mark of Darkness is Purging-staff aware).
  - Overlays active cooldowns directly on spellbook widgets while preserving your active Resource Pack sprites.
  - Glows ready spells in the spellbook and the Magic tab icon, with its own glow style.
  - Blends (or picks the strongest of) multiple screen-edge colours and showers spell icons from any screen edge.
  - Optional combat-only mode with configurable break re-remind (default 120s).
- **Inventory Cooldowns & Re-dose Protection**:
  - Imbued/Saturated Heart, Surge, divine/antifire/stamina/anti-poison/anti-venom/goading/menaphite/moonlight/prayer regeneration potions, and raid consumables (Overloads, Salts, Liquid adrenaline, Silk dressing, Blessed crystal scarab).
  - Preserve-aware re-dose estimates for regular and super combat boost potions, with left-click re-dose protection.
  - Ready highlight appears only when a cooldown you used finishes (single flash, pulse or solid; timed or permanent).
  - Combo-eat and Varlamore hunter-meat delayed-heal timers.
- **Health & Status Screen Overlays**:
  - Low HP vignette/pulse, damage hit flash (real damage only, not heals), poison/venom/disease glows, low prayer, freeze, idle, special attack ready, low run energy and unpotted-in-combat warnings.
  - Unpotted alert ignores alching and Magic XP from utility spells (thralls, Death Charge, Vengeance) and is suppressed while Overload / Smelling salts are active.
- **Every effect is individually switchable**: each warning and spell has its own Screen glow / Shower icon (and Spellbook glow) toggle, plus global *Allow screen glows*, *Allow icon showers* and *Allow item & spellbook glows* switches.
- **Lightweight rendering**: the vignette uses cached gradient paints (seamless rounded corners), nothing is drawn while no warning is active, and per-item name classification is memoised.

## Running in Developer Mode

```bash
./gradlew run
```

## Screenshots

### Spell Cooldowns & Resource Pack Outlines
![Spell Cooldowns and Outlines](docs/screenshots/spell-cooldowns.png)

### Magic Tab & Inventory Item Silhouette Glow
![Magic Tab and Item Glow](docs/screenshots/tab-and-item-glow.png)

### Spell Ready Screen-Edge Glow & Icon Shower
![Spell Icon Shower](docs/screenshots/spell-shower.jpg)

### Multi-Status Vignette & Combined Icon Shower
![Multi-Status Icon Shower](docs/screenshots/multi-status-shower.jpg)
