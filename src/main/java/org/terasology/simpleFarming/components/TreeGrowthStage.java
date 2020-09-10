// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.nui.reflection.MappedContainer;

/**
 * Represents a stage of growth for a tree.
 * <p>
 * Contains the positions of leaves relative to the root block and the number of logs this growth stage should contain.
 *
 * @see SaplingDefinitionComponent
 */
@MappedContainer
public class TreeGrowthStage {
    /**
     * Number of logs this stage should have.
     */
    public int height;

    /**
     * Prefab containing a {@link LeafStructureComponent} defining the structure of this stage's leaves.
     */
    public Prefab leafStructure;

    /**
     * Minimum time before this growth stage, in milliseconds.
     */
    public int minTime;

    /**
     * Maximum time before this growth stage, in milliseconds.
     */
    public int maxTime;

    /**
     * Default constructor required for persistence.
     */
    public TreeGrowthStage() {
    }

    /**
     * Construct a copy of the given {@code TreeGrowthStage}.
     */
    public TreeGrowthStage(TreeGrowthStage clone) {
        this.height = clone.height;
        this.leafStructure = clone.leafStructure;
        this.maxTime = clone.maxTime;
        this.minTime = clone.minTime;
    }
}
