// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.terasology.engine.world.block.Block;
import org.terasology.nui.reflection.MappedContainer;

/**
 * Represents a stage of growth for a bush.
 *
 * @see BushDefinitionComponent
 */
@MappedContainer
public class BushGrowthStage {
    /**
     * The block to use for this stage.
     */
    public Block block;

    /**
     * Minimum time before the next growth stage, in milliseconds.
     */
    public int minTime;

    /**
     * Maximum time before the next growth stage, in milliseconds.
     */
    public int maxTime;

    /**
     * Default constructor required for persistence.
     */
    public BushGrowthStage() {
    }

    /**
     * Construct a copy of the given {@code GrowthStage}.
     */
    public BushGrowthStage(BushGrowthStage clone) {
        this.block = clone.block;
        this.maxTime = clone.maxTime;
        this.minTime = clone.minTime;
    }
}
