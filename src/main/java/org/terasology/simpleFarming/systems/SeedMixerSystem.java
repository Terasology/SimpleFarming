/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.simpleFarming.systems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.genome.events.OnBreed;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.events.BeforeItemPutInInventory;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.SeedComponent;
import org.terasology.simpleFarming.components.SeedMixerComponent;

import java.util.List;

@RegisterSystem
public class SeedMixerSystem extends BaseComponentSystem {
    @In
    private InventoryManager inventoryManager;

    private static final Logger logger = LoggerFactory.getLogger(SeedMixerSystem.class);

    private InventoryComponent inventory;

    private int currentOffspringIndex;

    @ReceiveEvent
    public void filterSeed(BeforeItemPutInInventory event, EntityRef entity, SeedMixerComponent component) {
        if (!event.getItem().hasComponent(SeedComponent.class)) {
            event.consume();
        }
        if (event.getSlot() == 2 || event.getSlot() == 3) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void onSeedsAdded(BeforeItemPutInInventory event, EntityRef entity, SeedMixerComponent component) {
        inventory = entity.getComponent(InventoryComponent.class);
        if (event.getSlot() == 0 || event.getSlot() == 1) {
            EntityRef firstSeed = event.getItem();
            EntityRef secondSeed = inventory.itemSlots.get(1 - event.getSlot());
            if (!secondSeed.equals(EntityRef.NULL)) {
                EntityRef offspring1 = firstSeed.copy();
                EntityRef offspring2 = firstSeed.copy();
                logger.info("Sending events");
                currentOffspringIndex = 0;
                firstSeed.send(new OnBreed(firstSeed, secondSeed, offspring1));
                firstSeed.send(new OnBreed(firstSeed, secondSeed, offspring2));
            }
        }
    }

    @ReceiveEvent
    public void onSeedsBred(OnBreed event, EntityRef entity, SeedComponent seedComponent) {
        if (inventory != null) {
            EntityRef offspring = event.getOffspring();
            logger.info(offspring.getComponent(GenomeComponent.class).genes);
            inventory.itemSlots.set(currentOffspringIndex + 2, offspring);
            currentOffspringIndex += 1;
            if (currentOffspringIndex == 2) {
                event.getOrganism1().destroy();
                event.getOrganism2().destroy();
            }
        }
    }
}
