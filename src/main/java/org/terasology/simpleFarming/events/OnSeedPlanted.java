// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.events;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.gestalt.entitysystem.event.Event;

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

    public OnSeedPlanted(Vector3ic position) {
        rootPosition = new Vector3i(position);
    }

    public Vector3ic getPosition() {
        return rootPosition;
    }

    public void setPosition(Vector3ic position) {
        this.rootPosition.set(position);
    }
}
