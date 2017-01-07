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
import org.terasology.entitySystem.Component;

import java.util.ArrayList;
import java.util.Map;

/**
 * Defines the properties of a fruit tree, such as what fruit it grows and its appearance
 */
public class TreeDefinitionComponent implements Component {
    /**
     * The number of blocks high the trunk of the tree is
     */
    public int trunkHeight;

    /**
     * The URI of the block to be used as the trunk - Oak Trunk by default
     */
    public String trunkBlock = "Core:OakTrunk";

    /**
     * A list defining the length of each layer of the canopy
     * Each number in the list corresponds to one layer, starting from the base of the tree
     */
    public ArrayList<Integer> canopyLayers = new ArrayList<>();

    /**
     * A list defining the growth stages that the fruits borne by the tree will go through
     */
    public Map<String, TimeRange> fruitGrowthStages = Maps.newTreeMap();
}
