package com.combatauras.settings;

import com.combatauras.config.CombatAurasConfig;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds the current {@link SettingsSnapshot}. Rebuilt by the plugin on every change to its own config group
 * (a preset rebuilds once after all its keys are written; each build is a few hundred cached getter calls).
 */
@Singleton
public class Settings
{
    private final CombatAurasConfig config;
    private volatile SettingsSnapshot current;

    @Inject
    public Settings(CombatAurasConfig config)
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
