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

import org.terasology.reflection.MappedContainer;
import org.terasology.world.block.Block;

/**
 * Represents a stage of growth for a bush.
 *
 * @see BushDefinitionComponent
 */
@MappedContainer
public class GrowthStage {
    /** The block to use for this stage. */
    public Block block;

    /** Minimum time before the next growth stage, in milliseconds. */
    public int minTime;

    /** Maximum time before the next growth stage, in milliseconds. */
    public int maxTime;

    /** Default constructor required for persistence. */
    public GrowthStage() {
    }

    /** Construct a copy of the given {@code GrowthStage}. */
    public GrowthStage(GrowthStage clone) {
        this.block = clone.block;
        this.maxTime = clone.maxTime;
        this.minTime = clone.minTime;
    }
}
