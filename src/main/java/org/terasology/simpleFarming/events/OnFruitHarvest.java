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
package org.terasology.simpleFarming.events;

import org.terasology.entitySystem.event.Event;
import org.terasology.math.geom.Vector3i;

/**
 * An event triggered on the tree controller whenever a fruit is harvested.
 */
public class OnFruitHarvest implements Event {
    /**
     * The position of the fruit being harvested.
     */
    private Vector3i fruitPos;

    public OnFruitHarvest(Vector3i fruitPos) {
        this.fruitPos = fruitPos;
    }

    public Vector3i getPosition() {
        return fruitPos;
    }
}
