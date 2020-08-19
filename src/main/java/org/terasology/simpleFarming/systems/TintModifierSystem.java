// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.systems;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.simpleFarming.events.ModifyTint;
import org.terasology.substanceMatters.components.MaterialCompositionComponent;
import org.terasology.substanceMatters.components.MaterialItemComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class TintModifierSystem extends BaseComponentSystem {

    @ReceiveEvent
    public void onTintModified(ModifyTint event, EntityRef entity) {
        if (event.tintParameter > 1.5f) {
            MaterialItemComponent materialItemComponent = new MaterialItemComponent();
            materialItemComponent.icon = entity.getParentPrefab().getName();
            entity.addOrSaveComponent(materialItemComponent);
            MaterialCompositionComponent materialCompositionComponent = new MaterialCompositionComponent();
            materialCompositionComponent.addSubstance(getTinter(event.tintParameter), 1f);
            entity.addOrSaveComponent(materialCompositionComponent);
        }

    }

    private static String getTinter(float amount) {
        if (amount > 2.5f) {
            return "MaxTinter";
        } else if (amount > 2f) {
            return "ModerateTinter";
        } else {
            return "LowTinter";
        }
    }
}
