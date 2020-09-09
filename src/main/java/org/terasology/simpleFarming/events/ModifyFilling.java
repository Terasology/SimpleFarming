// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.events;

import org.terasology.engine.entitySystem.event.Event;
import org.terasology.engine.utilities.modifiable.ModifiableValue;

/**
 * Event sent to modify the filling of harvested produce.
 */
public class ModifyFilling implements Event {
    private final float newFilling;
    public ModifiableValue filling;

    public ModifyFilling(float newFilling) {
        this.newFilling = newFilling;
    }

    public float getNewFilling() {
        return newFilling;
    }
}
