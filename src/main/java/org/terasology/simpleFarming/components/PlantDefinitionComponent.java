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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.terasology.entitySystem.Component;
import org.terasology.reflection.MappedContainer;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.block.ForceBlockActive;

import java.util.List;
import java.util.Map;

@ForceBlockActive
public class PlantDefinitionComponent implements Component {
    // I would have prefered an plain list,  but serializing mapped containers is not yet possible.
    // An array is simulated with getGrowthStages until serializing complex items is possible
    public Map<String, TimeRange> growthStages = Maps.newTreeMap();
    public String soilCategory = "Soil";
    // the item that this block turns into upon destruction
    public String seedPrefab;
    // Allows a static name for the plant that doesnt change with the block
    public String plantName;

    public PlantDefinitionComponent() {
    }

    public PlantDefinitionComponent(PlantDefinitionComponent other) {
        growthStages = other.growthStages;
        soilCategory = other.soilCategory;
        seedPrefab = other.seedPrefab;
        plantName = other.plantName;
    }

    public PlantGrowth[] getGrowthStages() {
        List<PlantGrowth> output = Lists.newLinkedList();

        for (Map.Entry<String, TimeRange> item : growthStages.entrySet()) {
            output.add(new PlantGrowth(item.getKey(), item.getValue()));
        }
        return output.toArray(new PlantGrowth[output.size()]);
    }

    @MappedContainer
    public static class TimeRange {
        public long minRandom = 0;
        public long maxRandom = 5000;
        public long fixed = 10000;

        public long getTimeRange() {
            long total = fixed;
            Random random = new FastRandom();
            total += random.nextFloat(minRandom, maxRandom);
            return total;
        }
    }

    public static class PlantGrowth extends TimeRange {
        public String block;

        public PlantGrowth(String block, TimeRange timeRange) {
            this.block = block;
            this.minRandom = timeRange.minRandom;
            this.maxRandom = timeRange.maxRandom;
            this.fixed = timeRange.fixed;
        }
    }
}
