/*
 * Copyright 2016 MovingBlocks
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
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.VineDefinitionComponent;
import org.terasology.simpleFarming.events.OnVineGrowth;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import java.util.ArrayList;
import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class VineAuthoritySystem extends BaseComponentSystem {
    private static final String GROW_ACTION = "VINEGROWTH";
    private static final Logger logger = LoggerFactory.getLogger(VineAuthoritySystem.class);

    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    InventoryManager inventoryManager;
    @In
    DelayManager delayManager;
    @In
    EntityManager entityManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    PrefabManager prefabManager;

    /**
     * Defines what to do when a vine seed is planted.
     *
     * @param event                   The event corresponding to the seed being planted
     * @param seedItem                The seed item that was planted
     * @param vineDefinitionComponent The vine definition component defined in the seed prefab
     * @param itemComponent           The item component of the seed item
     */
    @ReceiveEvent
    public void onPlantSeed(ActivateEvent event, EntityRef seedItem, VineDefinitionComponent vineDefinitionComponent, ItemComponent itemComponent) {
        if (!event.getTarget().exists() || event.getTargetLocation() == null) {
            return;
        }

        Side surfaceSide = Side.inDirection(event.getHitNormal());
        if (surfaceSide != Side.TOP) {
            return;
        }

        Vector3i plantPosition = new Vector3i(event.getTargetLocation(), 0.5f);
        plantPosition = surfaceSide.getAdjacentPos(plantPosition);
        Block currentPlantBlock = worldProvider.getBlock(plantPosition);

        BlockComponent soilBlockComponent = event.getTarget().getComponent(BlockComponent.class);
        if (soilBlockComponent == null) {
            return;
        }
        Block soilBlock = soilBlockComponent.getBlock();

        if (soilBlock.getBlockFamily().hasCategory(vineDefinitionComponent.soilCategory) && currentPlantBlock == blockManager.getBlock(BlockManager.AIR_ID)) {
            event.consume();

            Block saplingBlock = blockManager.getBlock(vineDefinitionComponent.sapling);
            if (saplingBlock == blockManager.getBlock(BlockManager.AIR_ID)) {
                logger.error("Could not find sapling block");
                return;
            }

            VineDefinitionComponent newVineDefinitionComponent = new VineDefinitionComponent();
            newVineDefinitionComponent.nextGrowth = vineDefinitionComponent.nextGrowth;
            newVineDefinitionComponent.growthsTillRipe = vineDefinitionComponent.growthsTillRipe;
            newVineDefinitionComponent.soilCategory = vineDefinitionComponent.soilCategory;
            newVineDefinitionComponent.seedPrefab = seedItem.getParentPrefab().getName();
            newVineDefinitionComponent.plantName = vineDefinitionComponent.plantName;
            newVineDefinitionComponent.sapling = vineDefinitionComponent.sapling;
            newVineDefinitionComponent.trunk = vineDefinitionComponent.trunk;
            newVineDefinitionComponent.produce = vineDefinitionComponent.produce;
            newVineDefinitionComponent.isSapling = true;

            List<Vector3i> vineParts = new ArrayList<>();
            vineParts.add(plantPosition);
            newVineDefinitionComponent.parts = vineParts;

            worldProvider.setBlock(plantPosition, saplingBlock);
            EntityRef vineEntity = blockEntityRegistry.getBlockEntityAt(plantPosition);
            vineEntity.addComponent(newVineDefinitionComponent);
            scheduleVineGrowth(vineEntity, newVineDefinitionComponent);
            inventoryManager.removeItem(seedItem.getOwner(), seedItem, seedItem, true, 1);
        }
    }

    private void scheduleVineGrowth(EntityRef vineEntity, VineDefinitionComponent vineDefinitionComponent) {
        delayManager.addDelayedAction(vineEntity, GROW_ACTION, vineDefinitionComponent.nextGrowth.getTimeRange());
    }

    /**
     * Sends an "start new growth cycle" signal.
     *
     * @param event                   The delayed action trigger due to which this function is called
     * @param entity                  The vine entity whose growth action is to be triggered
     * @param vineDefinitionComponent The vine definition component of the vine entity
     * @param blockComponent          The block component of the vine entity
     */
    @ReceiveEvent
    public void scheduledVineGrowth(DelayedActionTriggeredEvent event, EntityRef entity, VineDefinitionComponent vineDefinitionComponent, BlockComponent blockComponent) {
        if (event.getActionId().equals(GROW_ACTION)) {
            entity.send(new OnVineGrowth());
        }
    }

    /**
     * Makes the vine grow when a growth signal is sent.
     *
     * @param event                   The growth signal that triggered the function
     * @param entity                  The vine entity to be grown
     * @param vineDefinitionComponent The vine definition component of the vine entity to be grown
     * @param blockComponent          The block component of the vine entity
     */
    @ReceiveEvent
    public void onPlantGrowth(OnVineGrowth event, EntityRef entity, VineDefinitionComponent vineDefinitionComponent, BlockComponent blockComponent) {
        if (delayManager.hasDelayedAction(entity, GROW_ACTION)) {
            delayManager.cancelDelayedAction(entity, GROW_ACTION);
        }

        if (!vineDefinitionComponent.isSapling) {
            // Prefer repairing vines if broken
            List<Vector3i> copy = new ArrayList<>(vineDefinitionComponent.parts);
            for (Vector3i position: copy) {
                if (worldProvider.getBlock(position) != blockManager.getBlock(vineDefinitionComponent.trunk)) {
                    if (worldProvider.getBlock(position) == blockManager.getBlock(BlockManager.AIR_ID)) {
                        Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
                        worldProvider.setBlock(position, trunkBlock);
                        scheduleVineGrowth(entity, vineDefinitionComponent);
                        return;
                    } else {
                        vineDefinitionComponent.parts.remove(position);
                    }
                }
            }

            Vector3i newPartPosition = getNewPartPosition(vineDefinitionComponent);

            if (newPartPosition == null) {
                return;
            }

            if (vineDefinitionComponent.growthsTillRipe <= 0) {
                if (Math.random() > 0.7) {
                    Block fruitBlock = blockManager.getBlock(vineDefinitionComponent.produce);
                    worldProvider.setBlock(newPartPosition, fruitBlock);
                    scheduleVineGrowth(entity, vineDefinitionComponent);
                    return;
                }
            }
            if (vineDefinitionComponent.parts.size() <= vineDefinitionComponent.maxParts) {
                Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
                worldProvider.setBlock(newPartPosition, trunkBlock);
                vineDefinitionComponent.parts.add(newPartPosition);
                vineDefinitionComponent.growthsTillRipe = vineDefinitionComponent.growthsTillRipe - 1;
            }
        } else {
            Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
            Vector3i pos = vineDefinitionComponent.parts.get(0);
            worldProvider.setBlock(pos, trunkBlock);
            vineDefinitionComponent.isSapling = false;
            EntityRef newPartEntity = blockEntityRegistry.getBlockEntityAt(pos);
            newPartEntity.addComponent(vineDefinitionComponent);
        }

        scheduleVineGrowth(entity, vineDefinitionComponent);
    }

    private Vector3i getNewPartPosition(VineDefinitionComponent vineDefinitionComponent) {
        java.util.Random randomGenerator = new java.util.Random();

        List<Vector3i> partsCopy = new ArrayList<>(vineDefinitionComponent.parts);

        Vector3i rand;
        List<Vector3i> possible;
        while (partsCopy.size() > 0) {
            possible = new ArrayList<>();
            rand = partsCopy.get(randomGenerator.nextInt(partsCopy.size()));

            if (worldProvider.getBlock((new Vector3i(rand)).add(1, 0, 0)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                if (worldProvider.getBlock((new Vector3i(rand)).add(1, -1, 0)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                    if (worldProvider.getBlock((new Vector3i(rand)).add(1, -2, 0)) != blockManager.getBlock(BlockManager.AIR_ID)) {
                        possible.add((new Vector3i(rand)).add(1, -1, 0));
                    }
                } else {
                    possible.add((new Vector3i(rand)).add(1, 0, 0));
                }
            }
            if (worldProvider.getBlock((new Vector3i(rand)).sub(1, 0, 0)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                if (worldProvider.getBlock((new Vector3i(rand)).sub(1, 1, 0)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                    if (worldProvider.getBlock((new Vector3i(rand)).sub(1, 2, 0)) != blockManager.getBlock(BlockManager.AIR_ID)) {
                        possible.add((new Vector3i(rand)).sub(1, 1, 0));
                    }
                } else {
                    possible.add((new Vector3i(rand)).sub(1, 0, 0));
                }
            }
            if (worldProvider.getBlock((new Vector3i(rand)).add(0, 0, 1)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                if (worldProvider.getBlock((new Vector3i(rand)).add(0, -1, 1)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                    if (worldProvider.getBlock((new Vector3i(rand)).add(0, -2, 1)) != blockManager.getBlock(BlockManager.AIR_ID)) {
                        possible.add((new Vector3i(rand)).add(0, -1, 1));
                    }
                } else {
                    possible.add((new Vector3i(rand)).add(0, 0, 1));
                }
            }
            if (worldProvider.getBlock((new Vector3i(rand)).sub(0, 0, 1)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                if (worldProvider.getBlock((new Vector3i(rand)).sub(0, 1, 1)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                    if (worldProvider.getBlock((new Vector3i(rand)).sub(0, 2, 1)) != blockManager.getBlock(BlockManager.AIR_ID)) {
                        possible.add((new Vector3i(rand)).sub(0, 1, 1));
                    }
                } else {
                    possible.add((new Vector3i(rand)).sub(0, 0, 1));
                }
            }

            if (possible.isEmpty()) {
                partsCopy.remove(rand);
            } else {
                return possible.get(randomGenerator.nextInt(possible.size()));
            }
        }
        return null;
    }
}
