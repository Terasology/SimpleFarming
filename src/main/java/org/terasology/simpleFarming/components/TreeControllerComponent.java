/*
 * Copyright 2016 MovingBlocks
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

import com.google.common.collect.Maps;
import org.terasology.engine.Time;
import org.terasology.entitySystem.Component;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.block.Block;
import org.terasology.world.block.ForceBlockActive;

import java.util.ArrayList;
import java.util.Map;

@ForceBlockActive
public class TreeControllerComponent implements Component {
    /**
     * A map defining the growth stages that the fruits borne by this tree will go through
     * and the time they will remain at this stage.
     */
    public Map<String, TimeRange> fruitGrowthStages = Maps.newTreeMap();

    /**
     * A map linking the positions of the fruits and their current growth stage.
     */
    public Map<Vector3i, Block> fruitBlocks;
}

