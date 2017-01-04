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
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.delay.DelayManager;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.PartOfTreeComponent;
import org.terasology.simpleFarming.components.PlantDefinitionComponent;
import org.terasology.simpleFarming.components.TimeRange;
import org.terasology.simpleFarming.components.TreeDefinitionComponent;
import org.terasology.simpleFarming.events.OnFruitCreated;
import org.terasology.simpleFarming.events.OnTreeGrow;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.terasology.simpleFarming.systems.FarmingAuthoritySystem.GROWTH_ACTION;

@RegisterSystem(RegisterMode.AUTHORITY)
public class TreeAuthoritySystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(TreeAuthoritySystem.class);

    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    DelayManager delayManager;

    /**
     * Called whenever a new tree is generated from a seed.
     *
     * @param event                    The event corresponding to the tree growing
     * @param entity                   The plant to be grown into a tree
     * @param treeDefinitionComponent  The component defining the properties of the tree, such as what fruit it bears
     * @param plantDefinitionComponent The component defining the properties of the plant before it grows into a tree
     * @param blockComponent           The component containing information about the plant block
     */
    @ReceiveEvent
    public void onNewTreeGrow(OnTreeGrow event, EntityRef entity, TreeDefinitionComponent treeDefinitionComponent, PlantDefinitionComponent plantDefinitionComponent, BlockComponent blockComponent) {
        Vector3i baseLocation = blockComponent.getPosition();

        PartOfTreeComponent trunkPart = new PartOfTreeComponent();
        trunkPart.isVital = true;

        List<Map.Entry<String, TimeRange>> growthStages = Lists.newLinkedList();
        for (Map.Entry<String, TimeRange> entry : treeDefinitionComponent.fruitGrowthStages.entrySet()) {
            growthStages.add(entry);
        }

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
            trunkPart.trunkBlocks.add(trunkPos);
        }
        // If the fruit blocks are obstructed, the tree will still grow, with only unobstructed fruits blocks appearing
        Iterator<Map.Entry<Vector3i, Block>> fruitBlocksIt = fruitBlocks.entrySet().iterator();
        while (fruitBlocksIt.hasNext()) {
            Vector3i fruitPos = fruitBlocksIt.next().getKey();
            Block targetBlock = worldProvider.getBlock(fruitPos);
            if ((!targetBlock.isReplacementAllowed() && targetBlock != blockComponent.getBlock())
                    || trunkBlocks.containsKey(fruitPos)) {
                fruitBlocksIt.remove();
            } else {
                trunkPart.fruitBlocks.add(fruitPos);
            }
        }

        worldProvider.setBlocks(fruitBlocks);
        worldProvider.setBlocks(trunkBlocks);

        // initialise fruit blocks
        for (Map.Entry<Vector3i, Block> entry : fruitBlocks.entrySet()) {
            EntityRef fruitEntity = blockEntityRegistry.getBlockEntityAt(entry.getKey());
            PlantDefinitionComponent fruitPlantDefinitionComponent = new PlantDefinitionComponent();
            fruitPlantDefinitionComponent.growthStages = treeDefinitionComponent.fruitGrowthStages;
            fruitPlantDefinitionComponent.plantName = treeDefinitionComponent.fruitName;
            fruitPlantDefinitionComponent.seedPrefab = plantDefinitionComponent.seedPrefab;

            fruitEntity.addComponent(fruitPlantDefinitionComponent);
            fruitEntity.send(new OnFruitCreated());
        }
        for (Map.Entry<Vector3i, Block> entry : trunkBlocks.entrySet()) {
            EntityRef trunkEntity = blockEntityRegistry.getBlockEntityAt(entry.getKey());
            trunkEntity.addComponent(trunkPart);
        }

        entity.removeComponent(PlantDefinitionComponent.class);
        entity.removeComponent(TreeDefinitionComponent.class);
    }

    /**
     * Handles any part of the tree being destroyed.
     * Whenever any vital part of a tree is destroyed, the fruits no longer grow
     * and the leaves are slowly destroyed a period of time.
     *
     * @param event  The event corresponding to the part of the tree being destroyed
     * @param entity The entity of the part of the tree being destroyed
     */
    @ReceiveEvent
    public void onPartOfTreeDestroyed(CreateBlockDropsEvent event, EntityRef entity, PartOfTreeComponent partOfTreeComponent, BlockComponent blockComponent) {
        if (partOfTreeComponent.isVital) {
            ArrayList<Vector3i> fruitBlocks = partOfTreeComponent.fruitBlocks;
            for (Vector3i fruitPos : fruitBlocks) {
                EntityRef fruitEntity = blockEntityRegistry.getBlockEntityAt(fruitPos);
                if (fruitEntity != EntityRef.NULL) {
                    if (delayManager.hasDelayedAction(fruitEntity, GROWTH_ACTION)) {
                        delayManager.cancelDelayedAction(fruitEntity, GROWTH_ACTION);
                    }
                }
            }
        }
    }
}
