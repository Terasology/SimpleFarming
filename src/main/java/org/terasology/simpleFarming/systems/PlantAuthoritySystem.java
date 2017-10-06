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
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;


@RegisterSystem(RegisterMode.AUTHORITY)
public class PlantAuthoritySystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(PlantAuthoritySystem.class);

    @In
    private InventoryManager inventoryManager;
    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;
    @In
    private BlockManager blockManager;

    private Block airBlock;

    @Override
    public void postBegin() {
        super.postBegin();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
    }

    @ReceiveEvent
    public void onSeedPlant(ActivateEvent event, EntityRef seed, SeedDefinitionComponent seedComponent) {
        /* The item is being used but not planted */
        if (event.getTargetLocation() == null || event.isConsumed()) {
            return;
        }
        Vector3i position = new Vector3i(event.getTargetLocation());
        position.add(Vector3i.up());
        if (Side.inDirection(event.getHitNormal()) == Side.TOP && isValidPosition(position)) {
            EntityRef plantEntity = entityManager.create(seedComponent.prefab);
            plantEntity.send(new OnSeedPlanted(position));
            inventoryManager.removeItem(seed.getOwner(), seed, seed, true, 1);
            event.consume();
        }
    }

    public boolean isValidPosition(Vector3i position) {
        if (position == null) {
            return false;
        }
        Block targetBlock = worldProvider.getBlock(position);
        if (targetBlock != airBlock) {
            return false;
        }
        Block belowBlock = worldProvider.getBlock(position.addY(-1));
        if (belowBlock.isPenetrable()) {
            return false;
        }
        return true;
    }
}
