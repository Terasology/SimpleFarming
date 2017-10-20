/*
 * Copyright 2015 MovingBlocks
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
import org.terasology.core.logic.random.Interval;
import org.terasology.entitySystem.Component;
import org.terasology.world.block.ForceBlockActive;
import java.util.Map;

@ForceBlockActive
public class PlantDefinitionComponent implements Component {
    /**
     * Map containing the growth stages of this plant. The string indicates the block URI, and the Interval indicates
     * the range of time necessary to move into the next growth stage.
     */
    public Map<String, Interval> growthStages = Maps.newTreeMap();

    /** The category of the soil */
    public String soilCategory = "Soil";

    /** The item that this block turns into upon destruction */
    public String seedPrefab;

    /** Allows a static name for the plant that doesnt change with the block */
    public String plantName;

    /** Whether the plant will become a tree after it has passed its growth stages */
    public boolean growsIntoTree;
}
