// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.events;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.Event;

/**
 * Sent to a plant just before it has been planted
 */
public class BeforePlanted implements Event {
    private EntityRef seed;

    public BeforePlanted(EntityRef seed) {
        this.seed = seed;
    }

    public EntityRef getSeed() {
        return seed;
    }
}
