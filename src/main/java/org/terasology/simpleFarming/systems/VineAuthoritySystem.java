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
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.TimeRange;
import org.terasology.simpleFarming.components.VineDefinitionComponent;
import org.terasology.simpleFarming.components.VinePartComponent;
import org.terasology.simpleFarming.events.OnVineGrowth;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
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

    Random random = new FastRandom();

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

            List<VinePartComponent> vineParts = new ArrayList<>();
            VinePartComponent rootComponent = new VinePartComponent();
            rootComponent.parentComponent = newVineDefinitionComponent;
            rootComponent.partPosition = plantPosition;
            vineParts.add(rootComponent);
            newVineDefinitionComponent.parts = vineParts;

            worldProvider.setBlock(plantPosition, saplingBlock);
            EntityRef vineEntity = blockEntityRegistry.getBlockEntityAt(plantPosition);
            vineEntity.addComponent(newVineDefinitionComponent);
            vineEntity.addComponent(rootComponent);
            scheduleVineGrowth(vineEntity, newVineDefinitionComponent);
            inventoryManager.removeItem(seedItem.getOwner(), seedItem, seedItem, true, 1);
        }
    }

    private void scheduleVineGrowth(EntityRef vineEntity, VineDefinitionComponent vineDefinitionComponent) {
        if (vineDefinitionComponent.parts.size() <= vineDefinitionComponent.maxParts) {
            delayManager.addDelayedAction(vineEntity, GROW_ACTION, vineDefinitionComponent.nextGrowth.getTimeRange());
        }
    }

    @ReceiveEvent
    public void scheduledVineGrowth(DelayedActionTriggeredEvent event, EntityRef entity, VineDefinitionComponent vineDefinitionComponent, BlockComponent blockComponent) {
        if (event.getActionId().equals(GROW_ACTION)) {
            entity.send(new OnVineGrowth());
        }
    }

    @ReceiveEvent
    public void onPlantGrowth(OnVineGrowth event, EntityRef entity, VineDefinitionComponent vineDefinitionComponent, BlockComponent blockComponent) {
        if (delayManager.hasDelayedAction(entity, GROW_ACTION)) {
            delayManager.cancelDelayedAction(entity, GROW_ACTION);
        }

        if (!vineDefinitionComponent.isSapling) {
            Vector3i newPartPosition = getNewPartPosition(vineDefinitionComponent.parts);

            if (vineDefinitionComponent.growthsTillRipe <= 0) {
                if (Math.random() > 0.75) {
                    Block fruitBlock = blockManager.getBlock(vineDefinitionComponent.produce);
                    worldProvider.setBlock(newPartPosition, fruitBlock);
                    scheduleVineGrowth(entity, vineDefinitionComponent);
                    return;
                }
            }

            Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
            worldProvider.setBlock(newPartPosition, trunkBlock);

            VinePartComponent newPartComponent = new VinePartComponent();
            newPartComponent.parentComponent = vineDefinitionComponent;
            newPartComponent.partPosition = newPartPosition;

            EntityRef newPartEntity = blockEntityRegistry.getBlockEntityAt(newPartPosition);
            newPartEntity.addComponent(newPartComponent);
            vineDefinitionComponent.parts.add(newPartComponent);
            vineDefinitionComponent.growthsTillRipe = vineDefinitionComponent.growthsTillRipe - 1;
        } else {
            Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
            worldProvider.setBlock(vineDefinitionComponent.parts.get(0).partPosition, trunkBlock);
            vineDefinitionComponent.isSapling = false;
        }

        scheduleVineGrowth(entity, vineDefinitionComponent);
    }

    @ReceiveEvent
    public void onDestroy(DoDestroyEvent event, EntityRef entity, VinePartComponent vinePartComponent, BlockComponent blockComponent) {
        vinePartComponent.parentComponent.parts.remove(vinePartComponent);
    }

    private Vector3i getNewPartPosition(List<VinePartComponent> parts) {
        java.util.Random randomGenerator = new java.util.Random();
        List<Vector3i> partsCopy = new ArrayList<>();
        for (VinePartComponent partComponent: parts) {
            partsCopy.add(partComponent.partPosition);
        }
        Vector3i rand;
        while (true) {
            rand = partsCopy.get(randomGenerator.nextInt(partsCopy.size()));
            if (worldProvider.getBlock((new Vector3i(rand)).add(1, 0, 0)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                return rand.add(1, 0, 0);
            } else if (worldProvider.getBlock((new Vector3i(rand)).sub(1, 0, 0)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                return rand.sub(1, 0, 0);
            } else if (worldProvider.getBlock((new Vector3i(rand)).add(0, 0, 1)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                return rand.add(0, 0, 1);
            } else if (worldProvider.getBlock((new Vector3i(rand)).sub(0, 0, 1)) == blockManager.getBlock(BlockManager.AIR_ID)) {
                return rand.sub(0, 0, 1);
            } else {
                partsCopy.remove(rand);
            }
        }
    }
}
