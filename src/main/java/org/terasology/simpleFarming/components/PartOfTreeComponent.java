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

import org.terasology.entitySystem.Component;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.block.ForceBlockActive;

import java.util.ArrayList;

/**
 * A component defining an entity as being part of a fruit tree.
 */
@ForceBlockActive
public class PartOfTreeComponent implements Component {
    /**
     * A list of the positions of the trunk blocks of the tree.
     */
    public ArrayList<Vector3i> trunkBlocks = new ArrayList<>();

    /**
     * A list of the positions of the fruit blocks of the tree.
     */
    public ArrayList<Vector3i> fruitBlocks = new ArrayList<>();

    /**
     * If a vital block of the tree is destroyed, all fruits will stop growing.
     * Trunk are vital blocks.
     */
    public boolean isVital;
}
