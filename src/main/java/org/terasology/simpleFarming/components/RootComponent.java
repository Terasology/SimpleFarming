// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.simpleFarming.systems.TreeAuthoritySystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the root (lowest log block) of a tree.
 * <p>
 * This component is automatically created by {@link TreeAuthoritySystem}. It
 * holds all of the information that relates to the tree in general as opposed
 * to a single block in specific.
 */
@ForceBlockActive
public class RootComponent implements Component<RootComponent> {

    /**
     * The block to use for the sapling in the world.
     */
    public Block sapling;

    /**
     * The block to use for a log in the tree in the world.
     */
    public Block log;

    /**
     * The prefab for the bush to use for the leaf block. This prefab should have
     * a {@link BushDefinitionComponent}
     */
    public Prefab leaf;

    /**
     * The stages which this tree should grow through, in order.
     */
    public List<TreeGrowthStage> growthStages = new ArrayList<>();

    /**
     * The current growth stage, represents the index in {@link #growthStages}.
     */
    public int growthStage = 0;

    /**
     * Holds references to all of the leaves in this tree. Note that leaves aren't
     * automatically removed from this set when they no longer exist, so any element
     * in this set should be verified with {@link EntityRef#exists()}.
     */
    public Set<EntityRef> leaves = new HashSet<>();

    /**
     * Whether or not the tree is alive. The tree is no longer alive if any of the
     * log blocks are destroyed, however leaves are not considered. The tree will
     * only be able to grow/ungrow (with a {@link CheatGrowthComponent}) if it is
     * alive.
     */
    public boolean alive = true;

    /**
     * Default constructor required for persistence.
     */
    public RootComponent() {

    }

    public RootComponent(SaplingDefinitionComponent base) {
        sapling = base.sapling;
        log = base.log;
        leaf = base.leaf;
        growthStages = base.growthStages;
    }

    @Override
    public void copy(RootComponent other) {
        this.sapling = other.sapling;
        this.log = other.log;
        this.leaf = other.leaf;
        this.growthStages = other.growthStages.stream()
                .map(TreeGrowthStage::new)
                .collect(Collectors.toList());
        this.growthStage = other.growthStage;
        this.leaves = other.leaves;
        this.alive = other.alive;
    }
}
