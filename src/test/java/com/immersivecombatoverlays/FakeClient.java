package com.immersivecombatoverlays;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;

/**
 * Minimal mutable fake of {@link Client} for tracker tests. Unmocked methods return type-appropriate defaults
 * (0 / false / null) instead of null, so primitive-returning calls never NPE.
 */
final class FakeClient
{
    final Map<Integer, Integer> varbits = new HashMap<>();
    final Map<Integer, Integer> varps = new HashMap<>();
    final Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
    final Map<Skill, Integer> real = new EnumMap<>(Skill.class);
    final Map<Integer, ItemContainer> containers = new HashMap<>();
    Object localPlayer;
    final Client client;

    FakeClient()
    {
        client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (proxy, m, a) -> handle(m, a)
        );
    }

    FakeClient varbit(int id, int value)
    {
        varbits.put(id, value);
        return this;
    }

    FakeClient varp(int id, int value)
    {
        varps.put(id, value);
        return this;
    }

    FakeClient level(Skill skill, int realLevel, int boostedLevel)
    {
        real.put(skill, realLevel);
        boosted.put(skill, boostedLevel);
        return this;
    }

    private Object handle(Method m, Object[] a)
    {
        switch (m.getName())
        {
            case "getVarbitValue":
                if (a.length == 1)
                {
                    return varbits.getOrDefault((Integer) a[0], 0);
                }
                break;
            case "getVarpValue":
                return varps.getOrDefault((Integer) a[0], 0);
            case "getBoostedSkillLevel":
                return boosted.getOrDefault((Skill) a[0], 1);
            case "getRealSkillLevel":
                return real.getOrDefault((Skill) a[0], 1);
            case "getItemContainer":
                if (a[0] instanceof Integer)
                {
                    return containers.get(a[0]);
                }
                return null;
            case "getLocalPlayer":
                return localPlayer;
            default:
                break;
        }
        return defaultValue(m.getReturnType());
    }

    static Object defaultValue(Class<?> type)
    {
        if (type == boolean.class)
        {
            return false;
        }
        if (type == int.class)
        {
            return 0;
        }
        if (type == long.class)
        {
            return 0L;
        }
        if (type == float.class)
        {
            return 0f;
        }
        if (type == double.class)
        {
            return 0d;
        }
        return null;
    }
}
