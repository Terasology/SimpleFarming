// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.events;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;

public class TransferGenomeEvent implements Event {
    private EntityRef transferEntity;

    public TransferGenomeEvent(EntityRef transferEntity) {
        this.transferEntity = transferEntity;
    }

    public EntityRef getTransferEntity() {
        return transferEntity;
    }
}
