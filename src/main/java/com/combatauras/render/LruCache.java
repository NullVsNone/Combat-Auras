package com.combatauras.render;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small bounded cache for rendered images: evicts the least recently used entry instead of clearing everything when
 * full, so a burst of new icons never forces every existing image to be rebuilt. Client thread only.
 */
public final class LruCache<K, V> extends LinkedHashMap<K, V>
{
    private final int maxEntries;

    public LruCache(int maxEntries)
    {
        super(Math.min(maxEntries, 64), 0.75f, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
    {
        return size() > maxEntries;
    }
}
