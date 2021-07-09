// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.joml.Vector3i;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.world.block.Block;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.simpleFarming.systems.TreeAuthoritySystem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores the necessary data for defining a tree.
 * <p>
 * In order to use it, add the component to a prefab, then link to that
 * prefab from a {@link SeedDefinitionComponent}. Alternatively, the component
 * can be included directly in the item and the SeedDefinitionComponent left
 * empty.
 * <p>
 * After the sapling has been placed, this component is transferred to
 * the sapling block's associated entity. When the sapling grows, the
 * {@link RootComponent} of a tree is created based on the sapling's
 * definition component.
 */
public class SaplingDefinitionComponent implements Component<SaplingDefinitionComponent> {

    /**
     * The location of the sapling. This does not need to be provided with the
     * definition, it is automatically set by {@link TreeAuthoritySystem}
     */
    public Vector3i location;

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
     * Default constructor required for persistence.
     */
    public SaplingDefinitionComponent() {

    }

    /**
     * Copy constructor to create a sapling from an existing tree. This is used when
     * the tree is un-grown into a sapling.
     *
     * @param base     The root to base this sapling off of.
     * @param location The location of the sapling in the world.
     * @see CheatGrowthComponent
     */
    public SaplingDefinitionComponent(RootComponent base, Vector3i location) {
        this.location = location;
        sapling = base.sapling;
        log = base.log;
        leaf = base.leaf;
        growthStages = base.growthStages;
    }

    @Override
    public void copy(SaplingDefinitionComponent other) {
        this.location = new Vector3i(other.location);
        sapling = other.sapling;
        log = other.log;
        leaf = other.leaf;
        this.growthStages = other.growthStages.stream()
                .map(TreeGrowthStage::new)
                .collect(Collectors.toList());
    }
}
