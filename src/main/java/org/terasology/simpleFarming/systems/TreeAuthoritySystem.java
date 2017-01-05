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

import com.google.common.collect.Lists;
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
import org.terasology.logic.health.BeforeDestroyEvent;
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.protobuf.EntityData;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.PlantDefinitionComponent;
import org.terasology.simpleFarming.components.TimeRange;
import org.terasology.simpleFarming.components.TreeControllerComponent;
import org.terasology.simpleFarming.components.TreeDefinitionComponent;
import org.terasology.simpleFarming.components.TreeFruitCreationComponent;
import org.terasology.simpleFarming.events.OnFruitHarvest;
import org.terasology.simpleFarming.events.OnNewTreeGrowth;
import org.terasology.simpleFarming.events.OnTreeGrowth;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class TreeAuthoritySystem extends BaseComponentSystem {
    private static final String TREE_GROWTH_ACTION = "TREEGROWTH";
    private static final String TREE_DEATH_ACTION = "TREEDEATH";
    private static final Logger logger = LoggerFactory.getLogger(TreeAuthoritySystem.class);

    /**
     * The minimum time taken after tree death for fruit blocks to disappear, in ms.
     */
    private static final long TREE_DEATH_MIN = 20000;
    /**
     * The maximum time taken after tree death for fruit blocks to disappear, in ms.
     */
    private static final long TREE_DEATH_MAX = 60000;

    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    DelayManager delayManager;
    @In
    InventoryManager inventoryManager;
    @In
    EntityManager entityManager;
    @In
    PrefabManager prefabManager;

    Random random = new FastRandom();

    /**
     * Called whenever a new tree is grown from a seed.
     *
     * @param event                    The event corresponding to the tree growing
     * @param entity                   The plant to be grown into a tree
     * @param treeDefinitionComponent  The component defining the properties of the tree, such as what fruit it bears
     * @param plantDefinitionComponent The component defining the properties of the plant before it grows into a tree
     * @param blockComponent           The component containing information about the plant block
     */
    @ReceiveEvent
    public void onNewTreeGrowth(OnNewTreeGrowth event, EntityRef entity, TreeDefinitionComponent treeDefinitionComponent, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        Vector3i baseLocation = blockComponent.getPosition();

        TreeControllerComponent treeController = new TreeControllerComponent();

        List<Map.Entry<String, TimeRange>> growthStages = Lists.newLinkedList();
        for (Map.Entry<String, TimeRange> entry : treeDefinitionComponent.fruitGrowthStages.entrySet()) {
            growthStages.add(entry);
        }
        TimeRange initialTimeRange = growthStages.get(0).getValue();

        Block fruitBlock = blockManager.getBlock(growthStages.get(0).getKey());
        if (fruitBlock == null) {
            logger.error("First stage fruit block does not exist");
            return;
        }
        Block trunkBlock = blockManager.getBlock(treeDefinitionComponent.trunkBlock);
        if (trunkBlock == null) {
            logger.error("Trunk block does not exist");
            return;
        }

        ArrayList<Integer> canopyLayers = treeDefinitionComponent.canopyLayers;
        Map<Vector3i, Block> trunkBlocks = new HashMap<>();
        Map<Vector3i, Block> fruitBlocks = new HashMap<>();

        for (int y = baseLocation.y(); y < baseLocation.y() + treeDefinitionComponent.trunkHeight; y++) {
            trunkBlocks.put(new Vector3i(baseLocation.x(), y, baseLocation.z()), trunkBlock);
        }
        int currentLayer = baseLocation.y();
        for (Integer layerLength : canopyLayers) {
            if (layerLength <= 0) {
                currentLayer++;
                continue;
            }
            int blocksFromTrunk = layerLength - 1;
            Vector3i canopyStart = new Vector3i(baseLocation.x() - blocksFromTrunk, currentLayer, baseLocation.z() - blocksFromTrunk);
            Region3i canopyLayerRegion = Region3i.createFromMinAndSize(canopyStart, new Vector3i(blocksFromTrunk * 2 + 1, 1, blocksFromTrunk * 2 + 1));
            for (Vector3i fruitPos : canopyLayerRegion) {
                fruitBlocks.put(fruitPos, fruitBlock);
            }
            currentLayer++;
        }

        // If there are any obstructions for the trunk, the tree will not grow
        for (Map.Entry<Vector3i, Block> entry : trunkBlocks.entrySet()) {
            Vector3i trunkPos = entry.getKey();
            Block targetBlock = worldProvider.getBlock(trunkPos);
            if (!targetBlock.isReplacementAllowed() && targetBlock != blockComponent.getBlock()) {
                return;
            }
        }
        // If the fruit blocks are obstructed, the tree will still grow, with only unobstructed fruits blocks appearing
        Iterator<Map.Entry<Vector3i, Block>> fruitBlocksIt = fruitBlocks.entrySet().iterator();
        while (fruitBlocksIt.hasNext()) {
            Vector3i fruitPos = fruitBlocksIt.next().getKey();
            Block targetBlock = worldProvider.getBlock(fruitPos);
            if ((!targetBlock.isReplacementAllowed() && targetBlock != blockComponent.getBlock())
                    || trunkBlocks.containsKey(fruitPos)) {
                fruitBlocksIt.remove();
            }
        }

        worldProvider.setBlocks(fruitBlocks);
        worldProvider.setBlocks(trunkBlocks);
        treeController.fruitBlocks = fruitBlocks;
        treeController.fruitGrowthStages = treeDefinitionComponent.fruitGrowthStages;

        EntityRef controllerEntity = blockEntityRegistry.getBlockEntityAt(baseLocation);
        controllerEntity.removeComponent(PlantDefinitionComponent.class);
        controllerEntity.removeComponent(TreeDefinitionComponent.class);
        controllerEntity.addComponent(treeController);
        scheduleTreeGrowth(entity, initialTimeRange.getTimeRange());
    }

    /**
     * Increase the growth stage of all fruit blocks on this tree by 1.
     *
     * @param event          The event corresponding to the tree growth event
     * @param entity         The entity corresponding to the tree controller
     * @param treeController The component defining the entity as being a tree controller
     */
    @ReceiveEvent
    public void onTreeGrowth(OnTreeGrowth event, EntityRef entity, TreeControllerComponent treeController) {
        Iterator<Map.Entry<Vector3i, Block>> fruitBlocksIt = treeController.fruitBlocks.entrySet().iterator();
        long timeToNextGrowth = 0;

        // increases growth stage of every fruit by 1
        while (fruitBlocksIt.hasNext()) {
            Map.Entry<Vector3i, Block> fruitBlock = fruitBlocksIt.next();
            Vector3i fruitPos = fruitBlock.getKey();
            Block blockAtFruitPos = worldProvider.getBlock(fruitPos);
            Block currentStageBlock = fruitBlock.getValue();
            Map.Entry<String, TimeRange> nextGrowthStage = findStageEntry(treeController, currentStageBlock, false);

            if (blockAtFruitPos != currentStageBlock) {
                fruitBlocksIt.remove();
                continue;
            }

            if (nextGrowthStage == null || nextGrowthStage.getKey().isEmpty() || nextGrowthStage.getValue() == null) {
                continue;
            }

            Block nextGrowthStageBlock = blockManager.getBlock(nextGrowthStage.getKey());
            TimeRange nextGrowthStageRange = nextGrowthStage.getValue();

            if (nextGrowthStageBlock == null) {
                continue;
            }

            worldProvider.setBlock(fruitPos, nextGrowthStageBlock);

            treeController.fruitBlocks.replace(fruitPos, nextGrowthStageBlock);
            entity.saveComponent(treeController);
            timeToNextGrowth = Math.max(timeToNextGrowth, nextGrowthStageRange.getTimeRange());
        }
        if (timeToNextGrowth > 0) {
            scheduleTreeGrowth(entity, timeToNextGrowth);
        }
    }

    @ReceiveEvent
    public void scheduledTreeGrowth(DelayedActionTriggeredEvent event, EntityRef entity, TreeControllerComponent treeControllerComponent) {
        if (event.getActionId().equals(TREE_GROWTH_ACTION)) {
            entity.send(new OnTreeGrowth());
        }
    }

    /**
     * Called whenever the player harvests a fruit from the tree.
     *
     * @param event              The event corresponding to the fruit block being activated by the player
     * @param fruitEntity        The entity corresponding to the harvestable fruit block
     * @param fruitItemComponent The component marking the fruit block as being harvestable
     * @param blockComponent     The component containing information about the fruit block
     */
    @ReceiveEvent
    public void onGiveFruit(ActivateEvent event, EntityRef fruitEntity, TreeFruitCreationComponent fruitItemComponent, BlockComponent blockComponent) {
        EntityRef instigator = event.getInstigator();

        if (!event.isConsumed() && instigator.hasComponent(InventoryComponent.class)) {
            EntityRef fruitItem = entityManager.create(fruitItemComponent.fruitItem);
            if (fruitItem != EntityRef.NULL) {
                inventoryManager.giveItem(instigator, instigator, fruitItem);
                event.consume();

                // find controller for this fruit
                EntityRef controllerEntity = EntityRef.NULL;
                for (EntityRef entity : entityManager.getEntitiesWith(TreeControllerComponent.class)) {
                    TreeControllerComponent controllerComponent = entity.getComponent(TreeControllerComponent.class);
                    if (controllerComponent.fruitBlocks.containsKey(blockComponent.getPosition())) {
                        controllerEntity = entity;
                        break;
                    }
                }
                if (controllerEntity != EntityRef.NULL) {
                    controllerEntity.send(new OnFruitHarvest(blockComponent.getPosition()));
                }
            }
        }
    }

    /**
     * Called to update the fruit block after it has been harvested.
     *
     * @param event          The event corresponding to the fruit block being harvested
     * @param entity         The entity corresponding to the controller block on the tree
     * @param treeController The component marking the entity as being a tree controller
     */
    @ReceiveEvent
    public void onFruitHarvest(OnFruitHarvest event, EntityRef entity, TreeControllerComponent treeController) {
        Vector3i fruitPos = event.getPosition();
        Block currentStageBlock = worldProvider.getBlock(fruitPos);
        if (currentStageBlock == null) {
            return;
        }
        Map.Entry<String, TimeRange> prevGrowthStage = findStageEntry(treeController, currentStageBlock, true);

        if (prevGrowthStage == null || prevGrowthStage.getKey().isEmpty() || prevGrowthStage.getValue() == null) {
            return;
        }

        Block prevGrowthStageBlock = blockManager.getBlock(prevGrowthStage.getKey());
        TimeRange prevGrowthStageRange = prevGrowthStage.getValue();

        if (fruitPos != null && prevGrowthStageBlock != null) {
            worldProvider.setBlock(fruitPos, prevGrowthStageBlock);
            treeController.fruitBlocks.replace(fruitPos, prevGrowthStageBlock);
            entity.saveComponent(treeController);
            scheduleTreeGrowth(entity, prevGrowthStageRange.getTimeRange());
        }
    }

    /**
     * Called whenever the controller block of the tree is destroyed.
     *
     * @param event               The event corresponding to the controller block being destroyed
     * @param entity              The entity corresponding to the controller block of the tree
     * @param controllerComponent The component marking the entity as being a tree controller
     */
    @ReceiveEvent
    public void onTreeDeath(DoDestroyEvent event, EntityRef entity, TreeControllerComponent controllerComponent) {
        if (delayManager.hasDelayedAction(entity, TREE_GROWTH_ACTION)) {
            delayManager.cancelDelayedAction(entity, TREE_GROWTH_ACTION);
        }
        for (Map.Entry<Vector3i, Block> entry : controllerComponent.fruitBlocks.entrySet()) {
            EntityRef fruitEntity = blockEntityRegistry.getBlockEntityAt(entry.getKey());
            delayManager.addDelayedAction(fruitEntity, TREE_DEATH_ACTION, random.nextLong(TREE_DEATH_MIN, TREE_DEATH_MAX));
        }
    }

    @ReceiveEvent
    public void onFruitDestroy(DelayedActionTriggeredEvent event, EntityRef entity) {
        if (event.getActionId().equals(TREE_DEATH_ACTION)) {
            entity.send(new DoDestroyEvent(null, null, EngineDamageTypes.DIRECT.get()));
        }
    }

    private Map.Entry<String, TimeRange> findStageEntry(TreeControllerComponent treeController, Block fruitBlock, boolean findPrevious) {
        List<Map.Entry<String, TimeRange>> fruitGrowthStages = Lists.newLinkedList();
        for (Map.Entry<String, TimeRange> item : treeController.fruitGrowthStages.entrySet()) {
            fruitGrowthStages.add(item);
        }
        for (int index = 0; index < fruitGrowthStages.size(); index++) {
            Map.Entry<String, TimeRange> growthStage = fruitGrowthStages.get(index);
            Block block = blockManager.getBlock(growthStage.getKey());
            if (block != null && fruitBlock == block) {
                if (findPrevious && index > 0) {
                    return fruitGrowthStages.get(index - 1);
                } else if (!findPrevious && index < fruitGrowthStages.size() - 1) {
                    return fruitGrowthStages.get(index + 1);
                }
            }
        }
        return null;
    }

    private void scheduleTreeGrowth(EntityRef controllerEntity, long timeToNextGrowth) {
        if (delayManager.hasDelayedAction(controllerEntity, TREE_GROWTH_ACTION)) {
            delayManager.cancelDelayedAction(controllerEntity, TREE_GROWTH_ACTION);
        }
        delayManager.addDelayedAction(controllerEntity, TREE_GROWTH_ACTION, timeToNextGrowth);
    }
}
