package com.combatauras.model;


/**
 * Continuous screen warnings. Each one carries the icon its shower uses (a sprite or an item, or none);
 * its settings are bound once in settings.Bindings.
 */
public enum StatusEffect
{
    DISEASE(1361, -1),
    POISON(1360, -1),
    VENOM(-1, 12913), // Anti-venom+(4)
    LOW_PRAYER(201, -1),
    LOW_HEALTH(203, -1),
    POISON_TICK(-1, -1),
    VENOM_TICK(-1, -1),
    DAMAGE(1359, -1),
    FREEZE(328, -1),
    IDLE(580, -1),
    SPEC_READY(168, -1),
    LOW_RUN_ENERGY(1070, -1),
    UNPOTTED(898, -1),
    // Message 1-3: chat-triggered glows (no icon shower)
    MESSAGE_1(-1, -1),
    MESSAGE_2(-1, -1),
    MESSAGE_3(-1, -1);

    private final int showerSpriteId;
    private final int showerItemId;

    StatusEffect(int showerSpriteId, int showerItemId)
    {
        this.showerSpriteId = showerSpriteId;
        this.showerItemId = showerItemId;
    }

    /** Sprite shown in the icon shower, or -1. */
    public int getShowerSpriteId()
    {
        return showerSpriteId;
    }

    /** Item shown in the icon shower, or -1. */
    public int getShowerItemId()
    {
        return showerItemId;
    }
}
