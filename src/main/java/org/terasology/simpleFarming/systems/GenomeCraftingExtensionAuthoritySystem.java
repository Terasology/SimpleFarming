// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.systems;

import org.terasology.crafting.events.OnRecipeCrafted;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.genome.GenomeRegistry;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.genome.system.SimpleGenomeManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

/**
 * Extension system integrating genetics with basic crafting.
 *
 * This component system is only loaded if both "Genome" and "BasicCrafting" are enabled.
 */
@RegisterSystem(value = RegisterMode.AUTHORITY, requiresOptional = {"Genome", "BasicCrafting"})
public class GenomeCraftingExtensionAuthoritySystem extends BaseComponentSystem {

    @In
    private GenomeRegistry genomeRegistry;

    /**
     * Adds genes to the crafted entity if breeding is possible.
     * @param event the OnRecipeCrafted event
     * @param entity the crafted entity which is to be modified
     */
    @ReceiveEvent
    public void onRecipeCraftedEvent(OnRecipeCrafted event, EntityRef entity) {
        EntityRef[] ingredients = event.getIngredients();
        if (ingredients.length != 2) {
            return;
        }

        if (!(ingredients[0].hasComponent(GenomeComponent.class) || ingredients[1].hasComponent(GenomeComponent.class))) {
            return;
        }

        SimpleGenomeManager genomeManager = new SimpleGenomeManager();
        boolean result = genomeManager.applyBreeding(ingredients[0], ingredients[1], entity);
        if (entity.hasComponent(GenomeComponent.class)) {
            GenomeUtil.updateFilling(genomeRegistry, entity);
        }
    }
}
