package com.immersivecombatoverlays.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-click setting bundles. Selecting a preset writes these values into this plugin's own config group
 * and then switches the dropdown back to CUSTOM, so every setting stays individually editable afterwards.
 * Keys not listed are left untouched (colours, thresholds, per-spell colours, etc.).
 */
public enum Preset
{
    CUSTOM("Custom (no change)"),
    MINIMAL("Minimal"),
    STANDARD("Standard (defaults)"),
    EVERYTHING("Everything");

    private final String label;

    Preset(String label)
    {
        this.label = label;
    }

    /**
     * @return key -> value to write; a null value means "reset to the default" (unset).
     */
    public Map<String, Object> settings()
    {
        Map<String, Object> v = new LinkedHashMap<>();
        switch (this)
        {
            case MINIMAL:
                // Subtle glows only: no showers, no tint, gentle motion, only the core spells
                v.put("allowScreenGlows", true);
                v.put("allowIconShowers", false);
                v.put("allowItemGlows", true);
                v.put("effectIntensity", 50);
                v.put("screenTint", 0);
                v.put("motionStrength", 30);
                v.put("damageGlow", false);
                v.put("idleEnabled", false);
                v.put("specEnabled", false);
                v.put("lowRunEnabled", false);
                v.put("spellWardOfArceuusEnabled", false);
                v.put("spellShadowVeilEnabled", false);
                v.put("spellCorruptionEnabled", false);
                v.put("spellVengeanceOtherEnabled", false);
                v.put("spellChargeEnabled", false);
                v.put("spellShowerMaxIcons", 6);
                break;
            case STANDARD:
                for (String key : MANAGED_KEYS)
                {
                    v.put(key, null);
                }
                break;
            case EVERYTHING:
                v.put("allowScreenGlows", true);
                v.put("allowIconShowers", true);
                v.put("allowItemGlows", true);
                v.put("effectIntensity", 85);
                v.put("screenTint", 25);
                v.put("motionStrength", 75);
                v.put("damageGlow", true);
                v.put("damageShower", true);
                v.put("idleEnabled", true);
                v.put("specEnabled", true);
                v.put("lowRunEnabled", true);
                v.put("spellWardOfArceuusEnabled", true);
                v.put("spellShadowVeilEnabled", true);
                v.put("spellCorruptionEnabled", true);
                v.put("spellVengeanceOtherEnabled", true);
                v.put("spellChargeEnabled", true);
                v.put("spellShowerMaxIcons", 20);
                break;
            default:
                break;
        }
        return Collections.unmodifiableMap(v);
    }

    /** Every key any preset touches; STANDARD resets all of them to their defaults. */
    public static final String[] MANAGED_KEYS = {
        "allowScreenGlows", "allowIconShowers", "allowItemGlows", "effectIntensity", "screenTint", "motionStrength",
        "damageGlow", "damageShower", "idleEnabled", "specEnabled", "lowRunEnabled",
        "spellWardOfArceuusEnabled", "spellShadowVeilEnabled", "spellCorruptionEnabled",
        "spellVengeanceOtherEnabled", "spellChargeEnabled", "spellShowerMaxIcons",
    };

    @Override
    public String toString()
    {
        return label;
    }
}
