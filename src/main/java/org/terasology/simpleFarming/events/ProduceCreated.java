// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
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
