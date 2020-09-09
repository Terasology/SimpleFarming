// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.events;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.Event;

/**
 * Transfers GenomeComponent of the calling entity to another entity used to enforce optional dependencies
 */
public class TransferGenomeEvent implements Event {
    /**
     * The entity to which the GenomeComponent is to be transferred
     */
    private final EntityRef transferEntity;

    public TransferGenomeEvent(EntityRef transferEntity) {
        this.transferEntity = transferEntity;
    }

    public EntityRef getTransferEntity() {
        return transferEntity;
    }
}
