package com.combatauras.render;

import java.util.Arrays;

/**
 * Bounded cache keyed by a primitive {@code long}, for lookups made every frame (images per item, spell widget ids).
 * <p>
 * 4-way set associative: a key can live in one of four slots of its set, and a miss on a full set evicts the least
 * recently used of those four. Unlike a {@code HashMap<Long, V>} a lookup never boxes its key or touches a linked
 * list, so steady-state frames allocate nothing. Null values are allowed (use {@link #contains} to tell "cached as
 * null" from "not cached"). Client thread only.
 */
public final class KeyedCache<V>
{
    private static final int WAYS = 4;

    private final long[] keys;
    private final long[] lastUsed;
    private final boolean[] used;
    private final Object[] values;
    private final int setMask;
    private long clock;

    /** @param capacity rounded up to a power of two, at least 4 */
    public KeyedCache(int capacity)
    {
        int sets = Integer.highestOneBit(Math.max(1, (Math.max(WAYS, capacity) + WAYS - 1) / WAYS) * 2 - 1);
        int size = sets * WAYS;
        keys = new long[size];
        lastUsed = new long[size];
        used = new boolean[size];
        values = new Object[size];
        setMask = sets - 1;
    }

    public int capacity()
    {
        return keys.length;
    }

    private int base(long key)
    {
        long h = key * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return ((int) h & setMask) * WAYS;
    }

    private int find(long key)
    {
        int b = base(key);
        for (int i = b; i < b + WAYS; i++)
        {
            if (used[i] && keys[i] == key)
            {
                lastUsed[i] = ++clock;
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public V get(long key)
    {
        int i = find(key);
        return i < 0 ? null : (V) values[i];
    }

    public boolean contains(long key)
    {
        return find(key) >= 0;
    }

    public void put(long key, V value)
    {
        int b = base(key);
        int victim = b;
        for (int i = b; i < b + WAYS; i++)
        {
            if (used[i] && keys[i] == key)
            {
                victim = i;
                break;
            }
            if (!used[i])
            {
                victim = i;
                break;
            }
            if (lastUsed[i] < lastUsed[victim])
            {
                victim = i;
            }
        }
        keys[victim] = key;
        values[victim] = value;
        used[victim] = true;
        lastUsed[victim] = ++clock;
    }

    public void clear()
    {
        Arrays.fill(used, false);
        Arrays.fill(values, null);
    }
}
