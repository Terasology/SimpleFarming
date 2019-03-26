/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.simpleFarming.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.simpleFarming.systems.TreeAuthoritySystem;
import org.terasology.world.block.Block;
import org.terasology.world.block.ForceBlockActive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the root (lowest log block) of a tree.
 * <p>
 * This component is automatically created by {@link TreeAuthoritySystem}. It
 * holds all of the information that relates to the tree in general as opposed
 * to a single block in specific.
 */
@ForceBlockActive
public class RootComponent implements Component {

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
}
