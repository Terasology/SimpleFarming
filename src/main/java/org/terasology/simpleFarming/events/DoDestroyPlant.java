// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.events;

import org.terasology.engine.entitySystem.event.Event;

/**
 * Sent in order to destroy a bush or a bud.
 *
 * @see org.terasology.simpleFarming.components.BushDefinitionComponent
 * @see org.terasology.simpleFarming.components.VineDefinitionComponent
 */
public class DoDestroyPlant implements Event {

    /**
     * Whether the parent vine is dead.  Relevant only for buds.
     */
    public boolean isParentDead;

    public DoDestroyPlant() {
    }

    public DoDestroyPlant(boolean isParentDead) {
        this.isParentDead = isParentDead;
    }
}
