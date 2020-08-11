


/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.simpleFarming.events;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;

/**
 * Sent when produce is harvested from a bush
 * This can be used to identify both the bush that was harvested as well as the produce
 */
public class ProduceCreated implements Event {
    private EntityRef creator;
    private EntityRef produce;

    public ProduceCreated(EntityRef creator, EntityRef produce) {
        this.creator = creator;
        this.produce = produce;
    }

    public EntityRef getCreator() {
        return creator;
    }

    public EntityRef getProduce() {
        return produce;
    }
}
