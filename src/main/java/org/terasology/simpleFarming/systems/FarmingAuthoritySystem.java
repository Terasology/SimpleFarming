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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.Side;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.PickupBuilder;
import org.terasology.math.geom.Vector3i;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.PlantDefinitionComponent;
import org.terasology.simpleFarming.components.PlantProduceComponent;
import org.terasology.simpleFarming.components.PlantProduceCreationComponent;
import org.terasology.simpleFarming.components.UnGrowPlantOnHarvestComponent;
import org.terasology.simpleFarming.events.OnPlantGrowth;
import org.terasology.simpleFarming.events.OnPlantHarvest;
import org.terasology.simpleFarming.events.OnPlantUnGrowth;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

import java.util.Map;

/**
 * Seeds contain the initial growth stages.  When planted, the plant definition is copied to the plant.
 * When the plant is broken, a seed with the same uri is created and the plant definition is copied back to the seed for its next use.
 * Harvesting a plant puts the produce directly in the instigator's inventory.
 * Growth happens by matching the current block with an item in the plant definition.  The next/previous stage can then replace the existing block.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class FarmingAuthoritySystem extends BaseComponentSystem {
    private static final String GROWTH_ACTION = "PLANTGROWTH";
    private static final Logger logger = LoggerFactory.getLogger(FarmingAuthoritySystem.class);

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
    public void onPlantSeed(ActivateEvent event, EntityRef seedItem, PlantDefinitionComponent plantDefinitionComponent, ItemComponent itemComponent) {
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

        if (soilBlock.getBlockFamily().hasCategory(plantDefinitionComponent.soilCategory) && currentPlantBlock == blockManager.getAir()) {
            event.consume();

            PlantDefinitionComponent.PlantGrowth growthStage = plantDefinitionComponent.getGrowthStages()[0];

            Block plantBlock = blockManager.getBlock(growthStage.block);
            if (plantBlock == blockManager.getAir()) {
                logger.error("Could not find plant: " + growthStage.block);
                return;
            }

            PlantDefinitionComponent newPlantDefinitionComponent = new PlantDefinitionComponent(plantDefinitionComponent);
            newPlantDefinitionComponent.seedPrefab = seedItem.getPrefabURI().toSimpleString();
            worldProvider.setBlock(plantPosition, plantBlock);
            EntityRef plantEntity = blockEntityRegistry.getBlockEntityAt(plantPosition);
            plantEntity.addComponent(newPlantDefinitionComponent);
            schedulePlantGrowth(plantEntity, growthStage);
            inventoryManager.removeItem(seedItem.getOwner(), seedItem, seedItem, true, 1);
        }
    }

    private Map.Entry<String, PlantDefinitionComponent.TimeRange> getGrowthStage(PlantDefinitionComponent plantDefinitionComponent, int index) {
        Map.Entry<String, PlantDefinitionComponent.TimeRange> growthStage = null;
        int i = 0;
        for (Map.Entry<String, PlantDefinitionComponent.TimeRange> item : plantDefinitionComponent.growthStages.entrySet()) {
            if (i == index) {
                return item;
            }
            i++;
        }
        return growthStage;
    }

    @ReceiveEvent
    public void onPlantDestroyed(CreateBlockDropsEvent event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        event.consume();
        EntityRef seedItem = entityManager.create(plantDefinitionComponent.seedPrefab);
        PickupBuilder pickupBuilder = new PickupBuilder(entityManager);
        Vector3f position = blockComponent.getPosition().toVector3f().add(0, 0.5f, 0);
        pickupBuilder.createPickupFor(seedItem, position, 6000, true);
        seedItem.send(new ImpulseEvent(random.nextVector3f(30.0f)));
    }

    private void schedulePlantGrowth(EntityRef entity, PlantDefinitionComponent.TimeRange timeRange) {
        delayManager.addDelayedAction(entity, GROWTH_ACTION, timeRange.getTimeRange());
    }

    @ReceiveEvent
    public void scheduledPlantGrowth(DelayedActionTriggeredEvent event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        if (event.getActionId().equals(GROWTH_ACTION)) {
            entity.send(new OnPlantGrowth());
        }
    }

    @ReceiveEvent
    public void onPlantGrowth(OnPlantGrowth event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        if (delayManager.hasDelayedAction(entity, GROWTH_ACTION)) {
            delayManager.cancelDelayedAction(entity, GROWTH_ACTION);
        }

        PlantDefinitionComponent.PlantGrowth nextGrowthStage = null;
        // find the next growth stage
        Block currentBlock = blockComponent.getBlock();
        for (int i = 0; i < plantDefinitionComponent.getGrowthStages().length - 1; i++) {
            PlantDefinitionComponent.PlantGrowth growthStage = plantDefinitionComponent.getGrowthStages()[i];
            Block block = blockManager.getBlock(growthStage.block);

            if (block.equals(currentBlock) && block != blockManager.getAir()) {
                nextGrowthStage = plantDefinitionComponent.getGrowthStages()[i + 1];
                break;
            }
        }

        if (nextGrowthStage == null) {
            return;
        }

        Block newPlantBlock = blockManager.getBlock(nextGrowthStage.block);
        if (newPlantBlock == blockManager.getAir()) {
            logger.error("Could not find the next growth stage block: " + nextGrowthStage.block);
            return;
        }

        blockEntityRegistry.setBlockRetainComponent(blockComponent.getPosition(), newPlantBlock, PlantDefinitionComponent.class);
        schedulePlantGrowth(entity, nextGrowthStage);
    }

    @ReceiveEvent
    public void onPlantUnGrowth(OnPlantUnGrowth event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        PlantDefinitionComponent.PlantGrowth nextGrowthStage = plantDefinitionComponent.getGrowthStages()[0];
        // find the previous growth stage
        Block currentBlock = blockComponent.getBlock();
        for (int i = 1; i < plantDefinitionComponent.getGrowthStages().length; i++) {
            PlantDefinitionComponent.PlantGrowth growthStage = plantDefinitionComponent.getGrowthStages()[i];
            Block block = blockManager.getBlock(growthStage.block);

            if (block.equals(currentBlock) && block != blockManager.getAir()) {
                nextGrowthStage = plantDefinitionComponent.getGrowthStages()[i - 1];
                break;
            }
        }

        Block newPlantBlock = blockManager.getBlock(nextGrowthStage.block);
        if (newPlantBlock == blockManager.getAir()) {
            logger.error("Could not find the next growth stage block: " + nextGrowthStage.block);
            return;
        }

        blockEntityRegistry.setBlockRetainComponent(blockComponent.getPosition(), newPlantBlock, PlantDefinitionComponent.class);
        schedulePlantGrowth(entity, nextGrowthStage);
    }

    @ReceiveEvent
    public void onHarvest(ActivateEvent event, EntityRef entity) {
        EntityRef target = event.getTarget();
        EntityRef instigator = event.getInstigator();
        if (target.exists() && instigator.exists()) {
            PlantProduceComponent plantProduceComponent = target.getComponent(PlantProduceComponent.class);
            if (plantProduceComponent != null) {
                inventoryManager.giveItem(instigator, target, plantProduceComponent.produceItem);
                plantProduceComponent.produceItem = EntityRef.NULL;
                target.saveComponent(plantProduceComponent);
                target.send(new OnPlantHarvest());
            }
        }
    }

    @ReceiveEvent
    public void onPlantProduceCreation(OnAddedComponent event, EntityRef entityRef, PlantProduceCreationComponent plantProduceCreationComponent) {
        EntityRef newItem = entityManager.create(plantProduceCreationComponent.producePrefab);
        PlantProduceComponent plantProduceComponent = new PlantProduceComponent(newItem);
        if (entityRef.hasComponent(PlantProduceComponent.class)) {
            entityRef.saveComponent(plantProduceComponent);
        } else {
            entityRef.addComponent(plantProduceComponent);
        }
    }

    @ReceiveEvent
    public void unGrowPlantOnHarvest(OnPlantHarvest event, EntityRef entityRef, UnGrowPlantOnHarvestComponent unGrowPlantOnHarvestComponent) {
        entityRef.send(new OnPlantUnGrowth());
    }

    @ReceiveEvent
    public void copySeedPlantDefinitionFromSeedToBlock(OnActivatedComponent event, EntityRef entityRef, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        if (plantDefinitionComponent.growthStages.isEmpty() && !plantDefinitionComponent.seedPrefab.isEmpty()) {
            Prefab seedPrefab = prefabManager.getPrefab(plantDefinitionComponent.seedPrefab);
            PlantDefinitionComponent seedPlantDefinition = seedPrefab.getComponent(PlantDefinitionComponent.class);
            if (seedPlantDefinition != null) {
                plantDefinitionComponent.growthStages = seedPlantDefinition.growthStages;
                plantDefinitionComponent.soilCategory = seedPlantDefinition.soilCategory;

                entityRef.saveComponent(plantDefinitionComponent);
            }
        }
    }
}
