package com.combatauras.tracker;

import javax.inject.Singleton;

/**
 * The three short-lived flash effects (damage taken, poison tick, venom tick), injected as one module.
 */
@Singleton
public class ImpactTrackers
{
    public final ImpactTracker damage = new ImpactTracker();
    public final ImpactTracker poisonTick = new ImpactTracker();
    public final ImpactTracker venomTick = new ImpactTracker();

    public void reset()
    {
        damage.reset();
        poisonTick.reset();
        venomTick.reset();
    }
}
