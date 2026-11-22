package com.combatauras;

import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * Worn-equipment facts some reminders depend on (Charge only matters with a god cape).
 */
@Singleton
public class EquipmentState
{
    private final Client client;

    @Inject
    public EquipmentState(Client client)
    {
        this.client = client;
    }

    public boolean isGodCapeWorn()
    {
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn == null)
        {
            return false;
        }
        Item cape = worn.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
        if (cape == null || cape.getId() <= 0)
        {
            return false;
        }
        return isGodCape(client.getItemDefinition(cape.getId()).getName());
    }

    /** Saradomin / Guthix / Zamorak capes, their imbued versions and the god max capes. */
    static boolean isGodCape(String name)
    {
        if (name == null)
        {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("cape") && (lower.contains("saradomin") || lower.contains("guthix") || lower.contains("zamorak"));
    }
}
