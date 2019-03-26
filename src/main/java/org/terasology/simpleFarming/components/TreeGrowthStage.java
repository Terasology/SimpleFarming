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

import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.reflection.MappedContainer;

/**
 * Represents a stage of growth for a tree.
 * <p>
 * Contains the positions of leaves relative to the root block and the number of logs this growth stage should contain.
 * 
 * @see SaplingDefinitionComponent
 */
@MappedContainer
public class TreeGrowthStage {
    /** Number of logs this stage should have. */
    public int height;

    /** Prefab containing a {@link LeafStructureComponent} defining the structure of this stage's leaves. */
    public Prefab leafStructure;

    /** Minimum time before this growth stage, in milliseconds. */
    public int minTime;

    /** Maximum time before this growth stage, in milliseconds. */
    public int maxTime;

    /** Default constructor required for persistence. */
    public TreeGrowthStage() {
    }

    /** Construct a copy of the given {@code TreeGrowthStage}. */
    public TreeGrowthStage(TreeGrowthStage clone) {
        this.height = clone.height;
        this.leafStructure = clone.leafStructure;
        this.maxTime = clone.maxTime;
        this.minTime = clone.minTime;
    }
}
