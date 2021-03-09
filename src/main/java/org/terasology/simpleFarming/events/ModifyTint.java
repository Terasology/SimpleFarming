// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.events;

import org.terasology.engine.entitySystem.event.Event;

public class ModifyTint implements Event {
    public float tintParameter;

    public ModifyTint(float tintParameter) {
        this.tintParameter = tintParameter;
    }
}
