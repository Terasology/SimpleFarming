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

import java.util.ArrayList;
import java.util.List;

public class VineDefinitionComponent implements Component {
    public String soilCategory = "Soil";

    public int maxParts = 20;

    public String seedPrefab;
    public String sapling;
    public String trunk;
    public String produce;

    public String plantName;
    public List<VinePartComponent> parts = new ArrayList<>();

    public boolean isSapling = true;

    public TimeRange nextGrowth;
    public int growthsTillRipe;
}
