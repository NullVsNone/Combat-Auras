package com.immersivecombatoverlays.settings;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds the current {@link SettingsSnapshot}. Rebuilt by the plugin on every change to its own config group
 * (a preset applying many keys simply rebuilds a few times; each build is a few hundred cached getter calls).
 */
@Singleton
public class Settings
{
    private final ImmersiveCombatOverlaysConfig config;
    private volatile SettingsSnapshot current;

    @Inject
    public Settings(ImmersiveCombatOverlaysConfig config)
    {
        this.config = config;
        this.current = SettingsSnapshot.of(config);
    }

    public SettingsSnapshot get()
    {
        return current;
    }

    public void refresh()
    {
        current = SettingsSnapshot.of(config);
    }
}
