// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.ForceBlockActive;

/**
 * Stores all data needed to grow a vine.
 * <p>
 * In contrast to {@link BushDefinitionComponent}, all of the properties on this component refer to
 * a type of vines.  A second component, {@link VineNodeComponent}, is used to track the status of a
 * particular vine in the world.
 * <p>
 * To create a vine, use an item with a {@link SeedDefinitionComponent} that links to the desired
 * vine prefab.  The newly created vine will initially consist of a single {@link #stem} block.
 * Over time, the vine will grow to adjacent blocks (in a line; each stem block has at most two
 * other stem blocks adjacent to it) and produce {@link #bud} blocks adjacent to the stem.  Each bud
 * is a bush, defined by a {@link BushDefinitionComponent} on the {@code #bud} prefab, and can be
 * interacted with like any other bush in order to produce produce and seeds.
 *
 * @see org.terasology.simpleFarming.systems.VineAuthoritySystem
 */
@ForceBlockActive
public class VineDefinitionComponent implements Component {

    /**
     * The block to use for a stem.
     */
    public Block stem;

    /**
     * The prefab of the bush to use as the bud.
     * <p>
     * This prefab should have a {@link BushDefinitionComponent} that defines the bud's properties.
     */
    public Prefab bud;

    /**
     * The minimum time between growth steps, in milliseconds.
     *
     * @see org.terasology.simpleFarming.systems.VineAuthoritySystem#onVineGrowth
     */
    public int minGrowTime;

    /**
     * The maximum time between growth steps, in milliseconds.
     *
     * @see org.terasology.simpleFarming.systems.VineAuthoritySystem#onVineGrowth
     */
    public int maxGrowTime;

    /**
     * The maximum length a vine can grow
     */
    public int maxLength = 20;
}
