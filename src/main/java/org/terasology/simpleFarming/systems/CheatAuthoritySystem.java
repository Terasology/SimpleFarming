/*
 * Copyright 2015 MovingBlocks
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

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.CheatGrowthComponent;
import org.terasology.simpleFarming.events.OnPlantGrowth;
import org.terasology.simpleFarming.events.OnPlantUnGrowth;
import org.terasology.world.BlockEntityRegistry;

/**
 * Handles the cheat grow mechanic of Growth Can & Ungrowth Can
 * Sends a OnPlantUnGrowth/OnPlantGrowth based on the item's causesUnGrowth property
 */

@RegisterSystem(RegisterMode.AUTHORITY)
public class CheatAuthoritySystem extends BaseComponentSystem {

    @In
    EntityManager entityManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    PrefabManager prefabManager;

    @ReceiveEvent
    public void onPlantSeed(ActivateEvent event, EntityRef item, CheatGrowthComponent cheatGrowthComponent, ItemComponent itemComponent) {
        if (!event.getTarget().exists() || event.getTargetLocation() == null) {
            return;
        }

        if (cheatGrowthComponent.causesUnGrowth) {
            event.getTarget().send(new OnPlantUnGrowth());
        } else {
            event.getTarget().send(new OnPlantGrowth());
        }
    }
}
