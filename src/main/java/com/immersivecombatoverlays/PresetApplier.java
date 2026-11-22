package com.immersivecombatoverlays;

import com.immersivecombatoverlays.model.Preset;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Applies a settings preset as one batch: every managed key is reset first (so the result never depends on the
 * previous preset), then the preset's values are written and the dropdown returns to CUSTOM. While a batch runs,
 * {@link #isApplying()} lets the plugin skip the per-key ConfigChanged work and rebuild once at the end.
 */
@Singleton
public class PresetApplier
{
    private final ConfigManager configManager;
    private boolean applying;

    @Inject
    public PresetApplier(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public boolean isApplying()
    {
        return applying;
    }

    /** @return true if anything was written */
    public boolean apply(String group, Preset preset)
    {
        if (preset == null || preset == Preset.CUSTOM)
        {
            return false;
        }
        applying = true;
        try
        {
            for (String key : Preset.MANAGED_KEYS)
            {
                configManager.unsetConfiguration(group, key);
            }
            for (Map.Entry<String, Object> e : preset.settings().entrySet())
            {
                if (e.getValue() != null)
                {
                    configManager.setConfiguration(group, e.getKey(), e.getValue());
                }
            }
            configManager.setConfiguration(group, "preset", Preset.CUSTOM);
        }
        finally
        {
            applying = false;
        }
        return true;
    }
}
