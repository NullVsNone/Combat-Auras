# Contributing

Bug reports and pull requests are welcome. Please use the issue templates; for timer problems the chat message
and what you expected matter most.

## Before opening a pull request
1. `./gradlew build` passes (Java 11).
2. New behaviour has a unit test. Trackers are tested headless through `FakeClient` (see `src/test`).
3. Settings follow the standard section layout (Enable at 0, Color 10, Screen glow 11, Shower 12, Notification 14,
   Reminder duration 15, Debug preview 19); `RemindersAndLayoutTest` enforces it.
4. No reflection, threads, file or network IO in plugin code (Plugin Hub rules).
5. Update `CHANGELOG.md`.

## Adding content
See "Adding content" in the README: one enum entry, its `@ConfigItem` block and one row in `settings/Bindings`.

## After a game update
Most data comes from RuneLite's generated `gameval` constants, so renamed or removed IDs fail compilation rather
than misbehaving silently. Check in this order:
1. `./gradlew build` against the new RuneLite release; fix any missing `VarbitID` / `VarPlayerID` / `ItemID` names.
2. New potions or food: run with debug logging and look for `Unrecognised potion` lines from `ConsumableCatalog`;
   add a rule in `PotionItems` or `FoodCooldownTracker.classify`.
3. Chat-driven timers (thrall, Mark of Darkness, Ward, Shadow Veil, parasite, freeze fallback): compare the
   message constants in the trackers with the game's wording.
4. Spell durations and cooldowns: compare with RuneLite's Timers & Buffs plugin.
