package com.immersivecombatoverlays.model;

/**
 * Potion and raid consumable effects tracked by varbit/varplayer or chat confirmation.
 * Mirrors the 27 effects from runelite-cd-overlay.
 */
public enum PotionEffect
{
    SURGE("Surge potion", -1, false, 1, 500, Category.COOLDOWN),
    OVERLOAD("Overload", 3953, false, 25, 500, Category.RAID),
    NMZ_OVERLOAD("NMZ Overload", 3953, false, 25, 500, Category.RAID),
    BLIGHTED_OVERLOAD("Blighted overload", 3953, false, 25, 500, Category.RAID),
    PRAYER_ENHANCE("Prayer enhance", 3954, false, 10, 485, Category.RAID),
    SMELLING_SALTS("Smelling salts", 14334, false, 25, 800, Category.RAID),
    LIQUID_ADRENALINE("Liquid adrenaline", 14333, false, 1, 250, Category.RAID),
    SILK_DRESSING("Silk dressing", -1, false, 1, 100, Category.RAID),
    BLESSED_CRYSTAL_SCARAB("Blessed crystal scarab", -1, false, 1, 100, Category.RAID),
    STAMINA("Stamina", 24, false, 10, 200, Category.EFFECT),
    GOADING("Goading", 11273, false, 1, 600, Category.EFFECT),
    MENAPHITE_REMEDY("Menaphite remedy", 14459, false, 1, 500, Category.EFFECT),
    MOONLIGHT("Moonlight", 10440, false, 1, 350, Category.RAID),
    PRAYER_REGENERATION("Prayer regeneration", 11362, false, 1, 800, Category.EFFECT),
    ANTIFIRE("Antifire", 3981, false, 10, 600, Category.EFFECT),
    SUPER_ANTIFIRE("Super antifire", 6101, false, 10, 300, Category.EFFECT),
    ANTIPOISON("Antipoison", 102, true, 1, 150, Category.EFFECT),
    ANTIVENOM("Anti-venom", 102, true, 1, 300, Category.EFFECT),
    DIVINE_ATTACK("Divine attack", 8429, false, 1, 500, Category.EFFECT),
    DIVINE_STRENGTH("Divine strength", 8430, false, 1, 500, Category.EFFECT),
    DIVINE_DEFENCE("Divine defence", 8431, false, 1, 500, Category.EFFECT),
    DIVINE_RANGING("Divine ranging", 8432, false, 1, 500, Category.EFFECT),
    DIVINE_MAGIC("Divine magic", 8433, false, 1, 500, Category.EFFECT),
    DIVINE_COMBAT("Divine super combat", 8430, false, 1, 500, Category.EFFECT),
    DIVINE_BASTION("Divine bastion", 13663, false, 1, 500, Category.EFFECT),
    DIVINE_BATTLEMAGE("Divine battlemage", 13664, false, 1, 500, Category.EFFECT),
    GUTHIX_REST("Guthix rest", -1, false, 1, 0, Category.EFFECT);

    public enum Category
    {
        COOLDOWN,
        EFFECT,
        RAID
    }

    private final String displayName;
    private final int varId;
    private final boolean varPlayer;
    private final int bucketTicks;
    private final int standardTicks;
    private final Category category;

    PotionEffect(
        String displayName,
        int varId,
        boolean varPlayer,
        int bucketTicks,
        int standardTicks,
        Category category
    )
    {
        this.displayName = displayName;
        this.varId = varId;
        this.varPlayer = varPlayer;
        this.bucketTicks = bucketTicks;
        this.standardTicks = standardTicks;
        this.category = category;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getVarId()
    {
        return varId;
    }

    public boolean isVarPlayer()
    {
        return varPlayer;
    }

    public int getBucketTicks()
    {
        return bucketTicks;
    }

    public int getStandardTicks()
    {
        return standardTicks;
    }

    public Category getCategory()
    {
        return category;
    }
}
