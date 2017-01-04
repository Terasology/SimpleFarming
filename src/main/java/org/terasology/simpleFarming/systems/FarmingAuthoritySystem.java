/**
 * Copyright 2015 MovingBlocks
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.simpleFarming.systems;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.common.DisplayNameComponent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.PartOfTreeComponent;
import org.terasology.simpleFarming.components.PlantDefinitionComponent;
import org.terasology.simpleFarming.components.PlantProduceComponent;
import org.terasology.simpleFarming.components.PlantProduceCreationComponent;
import org.terasology.simpleFarming.components.TimeRange;
import org.terasology.simpleFarming.components.TreeDefinitionComponent;
import org.terasology.simpleFarming.components.UnGrowPlantOnHarvestComponent;
import org.terasology.simpleFarming.events.OnFruitCreated;
import org.terasology.simpleFarming.events.OnPlantGrowth;
import org.terasology.simpleFarming.events.OnPlantHarvest;
import org.terasology.simpleFarming.events.OnPlantUnGrowth;
import org.terasology.simpleFarming.events.OnTreeGrow;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

import java.util.List;
import java.util.Map;

/**
 * Seeds contain the initial growth stages.  When planted, the plant definition is copied to the plant.
 * When the plant is broken, a seed with the same uri is created and the plant definition is copied back to the seed for its next use.
 * Harvesting a plant puts the produce directly in the instigator's inventory.
 * Growth happens by matching the current block with an item in the plant definition.  The next/previous stage can then replace the existing block.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class FarmingAuthoritySystem extends BaseComponentSystem {
    static final String GROWTH_ACTION = "PLANTGROWTH";
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
    /**
     * Handles the seed drop on plant destroyed event.
     *
     * @param event                     The corresponding event.
     * @param seedItem                  Reference to the seed entity.
     * @param plantDefinitionComponent  The definition of the plant.
     * @param itemComponent             The item component corresponding to the event
     */
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

        if (soilBlock.getBlockFamily().hasCategory(plantDefinitionComponent.soilCategory) && currentPlantBlock == blockManager.getBlock(BlockManager.AIR_ID)) {
            event.consume();

            List<Map.Entry<String, TimeRange>> growthStages = getGrowthStages(plantDefinitionComponent);
            TimeRange growthStage = growthStages.get(0).getValue();

            Block plantBlock = blockManager.getBlock(growthStages.get(0).getKey());
            if (plantBlock == blockManager.getBlock(BlockManager.AIR_ID)) {
                logger.error("Could not find plant: " + growthStages.get(0).getKey());
                return;
            }

            PlantDefinitionComponent newPlantDefinitionComponent = new PlantDefinitionComponent();
            newPlantDefinitionComponent.growthStages = plantDefinitionComponent.growthStages;
            newPlantDefinitionComponent.soilCategory = plantDefinitionComponent.soilCategory;
            newPlantDefinitionComponent.seedPrefab = seedItem.getParentPrefab().getName();
            newPlantDefinitionComponent.plantName = plantDefinitionComponent.plantName;
            newPlantDefinitionComponent.growsIntoTree = plantDefinitionComponent.growsIntoTree;

            worldProvider.setBlock(plantPosition, plantBlock);
            EntityRef plantEntity = blockEntityRegistry.getBlockEntityAt(plantPosition);
            plantEntity.addComponent(newPlantDefinitionComponent);

            TreeDefinitionComponent treeDefinitionComponent = seedItem.getComponent(TreeDefinitionComponent.class);
            if (treeDefinitionComponent != null) {
                plantEntity.addComponent(treeDefinitionComponent);
            }

            TreeDefinitionComponent tdp = plantEntity.getComponent(TreeDefinitionComponent.class);
            schedulePlantGrowth(plantEntity, growthStage);
            inventoryManager.removeItem(seedItem.getOwner(), seedItem, seedItem, true, 1);
        }
    }

    private Map.Entry<String, TimeRange> getGrowthStage(PlantDefinitionComponent plantDefinitionComponent, int index) {
        Map.Entry<String, TimeRange> growthStage = null;
        int i = 0;
        for (Map.Entry<String, TimeRange> item : plantDefinitionComponent.growthStages.entrySet()) {
            if (i == index) {
                return item;
            }
            i++;
        }
        return growthStage;
    }

    @ReceiveEvent
    /**
     * Handles the seed drop on plant destroyed event.
     *
     * @param event                     The event corresponding to the plant destroy.
     * @param entity                    Reference to the plant entity.
     * @param plantDefinitionComponent  The definition of the plant.
     * @param blockComponent            The block component corresponding to the event
     */
    public void onPlantDestroyed(CreateBlockDropsEvent event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        event.consume();
        EntityRef seedItem = entityManager.create(plantDefinitionComponent.seedPrefab);
        Vector3f position = blockComponent.getPosition().toVector3f().add(0, 0.5f, 0);
        seedItem.send(new DropItemEvent(position));
        seedItem.send(new ImpulseEvent(random.nextVector3f(30.0f)));
    }

    /**
     * Handles the growth schedule of plants
     *
     * @param entity    Reference to the plant entity.
     * @param timeRange The time where the plant grow to the next stage
     */
    private void schedulePlantGrowth(EntityRef entity, TimeRange timeRange) {
        delayManager.addDelayedAction(entity, GROWTH_ACTION, timeRange.getTimeRange());
    }

    @ReceiveEvent
    /**
     * Handles plant growth based on the schedule time from delayed action
     *
     * @param event                     The delayed action event.
     * @param entity                    The entity which is going to grown
     * @param plantDefinitionComponent  The definition of the plant.
     * @param blockComponent            The block component corresponding to the event
     */
    public void scheduledPlantGrowth(DelayedActionTriggeredEvent event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        if (event.getActionId().equals(GROWTH_ACTION)) {
            entity.send(new OnPlantGrowth());
        }
    }

    @ReceiveEvent
    /**
     * Handles plant growth event.
     *
     * @param event                     The event corresponding to the plant growth.
     * @param entity                    The entity which is going to grown
     * @param plantDefinitionComponent  The definition of the plant.
     * @param blockComponent            The block component corresponding to the event
     */
    public void onPlantGrowth(OnPlantGrowth event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        if (delayManager.hasDelayedAction(entity, GROWTH_ACTION)) {
            delayManager.cancelDelayedAction(entity, GROWTH_ACTION);
        }

        TimeRange nextGrowthStage = null;
        String nextGrowthStageBlockName = "";

        // Find the next growth stage
        List<Map.Entry<String, TimeRange>> growthStages = getGrowthStages(plantDefinitionComponent);
        Block currentBlock = blockComponent.getBlock();

        for (int i = 0; i < growthStages.size() - 1; i++) {
            TimeRange growthStage = growthStages.get(i).getValue();
            Block block = blockManager.getBlock(growthStages.get(i).getKey());

            if (block.equals(currentBlock) && block != blockManager.getBlock(BlockManager.AIR_ID)) {
                nextGrowthStage = growthStages.get(i + 1).getValue();
                nextGrowthStageBlockName = growthStages.get(i + 1).getKey();
                break;
            }
        }

        // If there's no next growth stage, return null, unless the plant grows into a tree.
        if (nextGrowthStage == null) {
            if (plantDefinitionComponent.growsIntoTree && entity.hasComponent(TreeDefinitionComponent.class)) {
                entity.send(new OnTreeGrow());
            }
            return;
        }

        // Find the next growth stage block information.
        Block newPlantBlock = blockManager.getBlock(nextGrowthStageBlockName);
        if (newPlantBlock == blockManager.getBlock(BlockManager.AIR_ID)) {
            // Log error if it can't find the next growth stage .block file.
            logger.error("Could not find the next growth stage block: " + nextGrowthStageBlockName);
            return;
        }

        TreeDefinitionComponent treeDefinitionComponent = entity.getComponent(TreeDefinitionComponent.class);

        // Grow the plant into the next growth stage.
        worldProvider.setBlock(blockComponent.getPosition(), newPlantBlock);

        // Creates new entity.
        EntityRef newEntity = blockEntityRegistry.getBlockEntityAt(blockComponent.getPosition());
        // Check the new entity for PlantDefinitionComponent.
        if (newEntity.hasComponent(PlantDefinitionComponent.class)) {
            newEntity.saveComponent(plantDefinitionComponent);
        } else {
            newEntity.addComponent(plantDefinitionComponent);
        }

        // Check for TreeDefinitionComponent
        if (treeDefinitionComponent != null) {
            if (newEntity.hasComponent(TreeDefinitionComponent.class)) {
                newEntity.saveComponent(treeDefinitionComponent);
            } else {
                newEntity.addComponent(treeDefinitionComponent);
            }
        }

        schedulePlantGrowth(newEntity, nextGrowthStage);
    }

    @ReceiveEvent
    /**
     * Handles plant ungrowth event.
     *
     * @param event                     The event corresponding to the plant ungrowth.
     * @param entity                    The entity which is going to ungrown.
     * @param plantDefinitionComponent  The definition of the plant.
     * @param blockComponent            The block component corresponding to the event.
     */
    public void onPlantUnGrowth(OnPlantUnGrowth event, EntityRef entity, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        List<Map.Entry<String, TimeRange>> growthStages = getGrowthStages(plantDefinitionComponent);

        if (growthStages.size() == 0) {
            return;
        }

        String previousGrowthStageBlockName = "";
        TimeRange previousGrowthStage = growthStages.get(0).getValue();

        // Find the previous growth stage.
        Block currentBlock = blockComponent.getBlock();
        for (int i = 1; i < growthStages.size(); i++) {
            TimeRange growthStage = growthStages.get(i).getValue();
            Block block = blockManager.getBlock(growthStages.get(i).getKey());

            if (block.equals(currentBlock) && block != blockManager.getBlock(BlockManager.AIR_ID)) {
                previousGrowthStage = growthStages.get(i - 1).getValue();
                previousGrowthStageBlockName = growthStages.get(i - 1).getKey();
                break;
            }
        }

        // In case there are no growth stages, or it was found that this is the first growth stage, return null.
        if (previousGrowthStage == null || previousGrowthStageBlockName.equals("")) {
            return;
        }
        // Find the previous growth stage block information.
        Block newPlantBlock = blockManager.getBlock(previousGrowthStageBlockName);
        if (newPlantBlock == blockManager.getBlock(BlockManager.AIR_ID)) {
            // Logs error if it can't find the previous growth stage .block file.
            logger.error("Could not find the previous growth stage block: " + previousGrowthStageBlockName);
            return;
        }

        // Change the block into the previous growth stage block.
        worldProvider.setBlock(blockComponent.getPosition(), newPlantBlock);
        // Creates a new entity
        EntityRef newEntity = blockEntityRegistry.getBlockEntityAt(blockComponent.getPosition());
        // Check the new entity for PlantDefinitionComponent.
        if (newEntity.hasComponent(PlantDefinitionComponent.class)) {
            newEntity.saveComponent(plantDefinitionComponent);
        } else {
            newEntity.addComponent(plantDefinitionComponent);
        }

        // Schedules a plant growth.
        schedulePlantGrowth(newEntity, previousGrowthStage);
    }

    @ReceiveEvent
    /**
     * Handles harvest event.
     * Gives player the produceItem to player.
     *
     * @param event     The event corresponding to the plant harvest
     * @param entity    The entity which is going to be harvested
     */
    public void onHarvest(ActivateEvent event, EntityRef entity) {
        EntityRef target = event.getTarget();
        EntityRef instigator = event.getInstigator();
        EntityRef harvestingEntity = entity;
        if (entity.equals(event.getTarget())) {
            harvestingEntity = instigator;
        }

        if (!event.isConsumed() && target.exists() && harvestingEntity.exists() && harvestingEntity.hasComponent(InventoryComponent.class)) {
            PlantProduceComponent plantProduceComponent = target.getComponent(PlantProduceComponent.class);
            if (plantProduceComponent != null) {
                EntityRef produceItem = plantProduceComponent.produceItem;
                plantProduceComponent.produceItem = EntityRef.NULL;
                target.saveComponent(plantProduceComponent);
                inventoryManager.giveItem(harvestingEntity, target, produceItem);
                target.send(new OnPlantHarvest());
                event.consume();
            }
        }
    }

    @ReceiveEvent
    /**
     * Handles plant produce creation event.
     *
     * @param event                         The event corresponding to plant produce creation.
     * @param entity                        Reference to the plant entity.
     * @param plantProduceCreationComponent The plant produce creation component corresponding to the event.
     */
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
    public void onFruitCreated(OnFruitCreated event, EntityRef fruitEntity, PlantDefinitionComponent plantDefinitionComponent) {
        List<Map.Entry<String, TimeRange>> growthStages = getGrowthStages(plantDefinitionComponent);
        TimeRange growthStage = growthStages.get(0).getValue();
        schedulePlantGrowth(fruitEntity, growthStage);
    }

    @ReceiveEvent
    /**
     * Handles ungrowing of plan after harvest.
     *
     * @param event                         The event corresponding to plant produce creation.
     * @param EntityRef                     Reference to the plant entity.
     * @param unGrowPlantOnHarvestComponent The ungrow plant on harvest component corresponding to the event.
     */
    public void unGrowPlantOnHarvest(OnPlantHarvest event, EntityRef entityRef, UnGrowPlantOnHarvestComponent unGrowPlantOnHarvestComponent) {
        entityRef.send(new OnPlantUnGrowth());
    }

    @ReceiveEvent
    /**
     * Handles copying plant definition from seed to block
     *
     * @param event                     The event corresponding to the plant.
     * @param EntityRef                 Reference to the plant entity.
     * @param plantDefinitionComponent  The definition of the plant.
     * @param blockComponent            The block component corresponding to the event.
     */
    public void copySeedPlantDefinitionFromSeedToBlock(OnActivatedComponent event, EntityRef entityRef,
                                                       PlantDefinitionComponent plantDefinitionComponent,
                                                       BlockComponent blockComponent) {
        updatePlantDefinitionAndDisplayName(entityRef, plantDefinitionComponent);
    }

    @ReceiveEvent
    public void copySeedPlantDefinitionFromSeedToBlock(OnChangedComponent event, EntityRef entityRef,
                                                       PlantDefinitionComponent plantDefinitionComponent,
                                                       BlockComponent blockComponent) {
        updatePlantDefinitionAndDisplayName(entityRef, plantDefinitionComponent);
    }

    /**
     * Update the plant definition and display name.
     *
     * @param entityRef                Reference to the plant entity.
     * @param plantDefinitionComponent The definition of the plant.
     */
    void updatePlantDefinitionAndDisplayName(EntityRef entityRef, PlantDefinitionComponent plantDefinitionComponent) {
        // Ensure that the important bits get copied from the seed prefab
        if (plantDefinitionComponent.growthStages.isEmpty() && !plantDefinitionComponent.seedPrefab.isEmpty()) {
            Prefab seedPrefab = prefabManager.getPrefab(plantDefinitionComponent.seedPrefab);
            PlantDefinitionComponent seedPlantDefinition = seedPrefab.getComponent(PlantDefinitionComponent.class);
            if (seedPlantDefinition != null) {

                PlantDefinitionComponent updatedPlantDefinitionComponent = new PlantDefinitionComponent();
                updatedPlantDefinitionComponent.growthStages = seedPlantDefinition.growthStages;
                updatedPlantDefinitionComponent.soilCategory = seedPlantDefinition.soilCategory;
                updatedPlantDefinitionComponent.plantName = seedPlantDefinition.plantName;

                // Recopy the block's seed prefab in case the seed does not supply one. Not ideal.
                updatedPlantDefinitionComponent.seedPrefab = plantDefinitionComponent.seedPrefab;
                entityRef.saveComponent(updatedPlantDefinitionComponent);
            }
        }

        // Update the name of the block if there is a static name present
        if (plantDefinitionComponent.plantName != null) {
            DisplayNameComponent displayNameComponent = entityRef.getComponent(DisplayNameComponent.class);
            if (displayNameComponent == null) {
                displayNameComponent = new DisplayNameComponent();
                displayNameComponent.name = plantDefinitionComponent.plantName;
                entityRef.addComponent(displayNameComponent);
            } else if (!displayNameComponent.name.equals(plantDefinitionComponent.plantName)) {
                displayNameComponent.name = plantDefinitionComponent.plantName;
                entityRef.saveComponent(displayNameComponent);
            }
        }
    }

    /**
     * Return the growth stages of this plant definition in list format.
     *
     * @param p The definition of the plant.
     */
    private List<Map.Entry<String, TimeRange>> getGrowthStages(PlantDefinitionComponent p) {
        List<Map.Entry<String, TimeRange>> output = Lists.newLinkedList();
        for (Map.Entry<String, TimeRange> item : p.growthStages.entrySet()) {
            output.add(item);
        }

        return output;
    }
}

