// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import com.google.common.collect.Maps;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.ForceBlockActive;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Stores all data needed to grow a bush.
 * <p>
 * Some of these properties apply to all bushes of a given type (e.g., all blueberry bushes) and are generally specified
 * on a prefab.  Others are specific to a particular bush and will be managed at runtime.  See individual fields for
 * more information on how they are used.
 * <p>
 * To create a bush, use an item with a {@link SeedDefinitionComponent} that links to the desired bush prefab.  The
 * newly created bush will then progress through its {@link #growthStages} over time.  Once it reaches the last stage,
 * it can be harvested (via the "use" action) to yield {@link #produce} or destroyed to yield {@link #seed}s.
 * <p>
 * This component is also used to define vine "buds".  These behave similarly to bushes, but grow off of vines.  See
 * {@link VineDefinitionComponent} for more details.
 *
 * @see org.terasology.simpleFarming.systems.BushAuthoritySystem
 */
@ForceBlockActive
public class BushDefinitionComponent implements Component {

    /**
     * Map associating prefabs with growth stages.  Generally specified by a prefab.
     * <p>
     * Keys are block names and values are {@link BushGrowthStage} objects.
     * <p>
     * The order in which entries occur is significant.  A bush will start in the first stage and progress forward
     * through the list.  When it reaches its final stage, it can be harvested (via the "use" action) to yield {@link
     * #produce}.
     */
    public Map<String, BushGrowthStage> growthStages = Maps.newTreeMap();

    /**
     * Whether the bush should survive being harvested.  Defaults to true; specified by prefab.
     * <p>
     * If this is true (the default), then harvesting yields {@link #produce} and resets the bush back one growth stage.
     * Otherwise, harvesting yields both {@code produce} and {@link #seed seeds} but destroys the bush.
     */
    public boolean sustainable = true;

    /**
     * The prefab name to use for seed drops.  Generally specified by a prefab.
     * <p>
     * These are dropped when the bush is destroyed when in its last growth stage.
     * <p>
     * This field may be null.  If it is, then the {@link #produce} prefab will also be used for seeds.  (This typically
     * represents things like potatoes, which are both produce and seed.)
     */
    public String seed;

    /**
     * Determines the chance of each amount of seed dropping.
     * <p>
     * The value at each index is the "weight" of that amount of seeds dropping. For example, if the value is [1, 2, 1],
     * then there is a 25% chance of 0 seeds, 50% chance of 1 seed, and 25% chance of 2 seeds.
     */
    public List<Integer> seedDropChances = Arrays.asList(0, 1, 1, 1);

    /**
     * The prefab name to use for produce drops.  Generally specified by a prefab.
     * <p>
     * These are dropped when the bush is harvested.  This can only be done when the bush is in its last growth stage.
     */
    public String produce;

    /**
     * The index of the current stage for this particular bush.
     */
    public int currentStage;

    /**
     * Used by vine buds to refer to the parent stem.
     */
    public EntityRef parent;
}
