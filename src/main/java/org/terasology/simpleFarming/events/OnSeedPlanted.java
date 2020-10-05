// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.events;

import org.joml.Vector3i;
import org.terasology.entitySystem.event.Event;

/**
 * Sent to a new bush or vine immediately after it has been planted.
 */
public class OnSeedPlanted implements Event {

    /**
     * The position of the new bush or vine root.
     * <p>
     * This is the air block above the block that the player targeted with the seed.
     */
    private Vector3i rootPosition;

    public OnSeedPlanted(Vector3i position) {
        rootPosition = position;
    }

    public Vector3i getPosition() {
        return rootPosition;
    }

    public void setPosition(Vector3i position) {
        this.rootPosition = position;
    }
}
