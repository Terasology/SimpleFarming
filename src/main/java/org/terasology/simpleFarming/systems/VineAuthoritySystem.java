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
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Direction;
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
import java.util.Collections;
import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class VineAuthoritySystem extends BaseComponentSystem {
    private static final String GROW_ACTION = "ACTION_VINEGROWTH";
    private static final Logger logger = LoggerFactory.getLogger(VineAuthoritySystem.class);
    private static final List<Direction> HORIZONTAL_DIRECTIONS = Lists.newArrayList(Direction.FORWARD, Direction.BACKWARD, Direction.LEFT, Direction.RIGHT);

    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private InventoryManager inventoryManager;
    @In
    private DelayManager delayManager;
    @In
    private EntityManager entityManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private PrefabManager prefabManager;

    private Block airBlock;

    @Override
    public void postBegin() {
        super.postBegin();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
    }

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
        Vector3i plantPosition = findPlantPosition(event, vineDefinitionComponent.soilCategory);
        if (plantPosition != null) {
            event.consume();

            Block saplingBlock = blockManager.getBlock(vineDefinitionComponent.sapling);
            if (saplingBlock == airBlock) {
                logger.error("Could not find sapling block");
                return;
            }

            EntityRef vineEntity = plantVineAtPosition(vineDefinitionComponent, plantPosition, saplingBlock);

            scheduleVineGrowth(vineEntity, vineDefinitionComponent.nextGrowth.sample());

            inventoryManager.removeItem(seedItem.getOwner(), seedItem, seedItem, true, 1);
        }
    }

    private EntityRef plantVineAtPosition(VineDefinitionComponent vineDefinitionComponent, Vector3i plantPosition, Block saplingBlock) {
        VineDefinitionComponent vineComponent = new VineDefinitionComponent(vineDefinitionComponent);
        vineComponent.isSapling = true;

        List<Vector3i> vineParts = new ArrayList<>();
        vineParts.add(plantPosition);
        vineComponent.parts = vineParts;

        worldProvider.setBlock(plantPosition, saplingBlock);
        EntityRef vineEntity = blockEntityRegistry.getBlockEntityAt(plantPosition);
        vineEntity.addComponent(vineComponent);
        return vineEntity;
    }

    private Vector3i findPlantPosition(ActivateEvent event, String soilCategory) {
        //target location required for placement
        if (!event.getTarget().exists() || event.getTargetLocation() == null) {
            return null;
        }

        //placement only allowed on top of a block
        Side surfaceSide = Side.inDirection(event.getHitNormal());
        if (surfaceSide != Side.TOP) {
            return null;
        }

        //the replaced block must be air
        Vector3i plantPosition = surfaceSide.getAdjacentPos(new Vector3i(event.getTargetLocation()));
        Block currentPlantBlock = worldProvider.getBlock(plantPosition);
        if (currentPlantBlock != airBlock) {
            return null;
        }

        //the activated entity must be a block with a valid soil category
        BlockComponent blockComponent = event.getTarget().getComponent(BlockComponent.class);
        if (blockComponent == null) {
            return null;
        }
        Block soilBlock = blockComponent.getBlock();
        if (soilCategory != null && !soilBlock.getBlockFamily().hasCategory(soilCategory)) {
            return null;
        }
        return plantPosition;
    }

    private void scheduleVineGrowth(EntityRef vineEntity, long delayMs) {
        delayManager.addDelayedAction(vineEntity, GROW_ACTION, delayMs);
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
        doVineGrowth(entity, vineDefinitionComponent);
        scheduleVineGrowth(entity, vineDefinitionComponent.nextGrowth.sample());
    }

    private void doVineGrowth(EntityRef entity, VineDefinitionComponent vineDefinitionComponent) {
        if (delayManager.hasDelayedAction(entity, GROW_ACTION)) {
            delayManager.cancelDelayedAction(entity, GROW_ACTION);
        }

        //sapling stage : replace the sapling with a trunk
        if (vineDefinitionComponent.isSapling) {
            growSaplingToTrunk(vineDefinitionComponent);
            return;
        }

        //special case: a block in the trunk was destroyed and is now air -> re-grow that block
        Vector3i brokenTrunkPosition = findBrokenTrunkBlockPosition(vineDefinitionComponent);
        Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
        if (brokenTrunkPosition != null) {
            worldProvider.setBlock(brokenTrunkPosition, trunkBlock);
            return;
        }

        //find a valid position for the new vine part.
        Vector3i newPartPosition = searchNewPartPosition(vineDefinitionComponent.parts, HORIZONTAL_DIRECTIONS, null);
        if (newPartPosition == null) {
            return;
        }

        //if the vine is ripe, check the random threshold to grow a new fruit
        if (vineDefinitionComponent.growthsTillRipe <= 0 && Math.random() > vineDefinitionComponent.probabilityThresholdForFruit) {
            Block fruitBlock = blockManager.getBlock(vineDefinitionComponent.produce);
            worldProvider.setBlock(newPartPosition, fruitBlock);
            return;
        }
        //if the vine is not fully grown, add a new part to it
        if (vineDefinitionComponent.parts.size() <= vineDefinitionComponent.maxParts) {
            worldProvider.setBlock(newPartPosition, trunkBlock);
            vineDefinitionComponent.parts.add(newPartPosition);
            if (vineDefinitionComponent.growthsTillRipe > 0) {
                vineDefinitionComponent.growthsTillRipe--;
            }
        }
    }

    private void growSaplingToTrunk(VineDefinitionComponent vineDefinitionComponent) {
        Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
        Vector3i pos = vineDefinitionComponent.parts.get(0);
        worldProvider.setBlock(pos, trunkBlock);
        vineDefinitionComponent.isSapling = false;
        EntityRef newPartEntity = blockEntityRegistry.getBlockEntityAt(pos);
        newPartEntity.addComponent(vineDefinitionComponent);
    }

    private Vector3i findBrokenTrunkBlockPosition(VineDefinitionComponent vineDefinitionComponent) {
        List<Vector3i> copy = new ArrayList<>(vineDefinitionComponent.parts);
        Block trunkBlock = blockManager.getBlock(vineDefinitionComponent.trunk);
        for (Vector3i position : copy) {
            if (worldProvider.getBlock(position) != trunkBlock) {
                if (worldProvider.getBlock(position) == airBlock) {
                    return position;
                }
            }
        }
        return null;
    }

    /**
     * Searches for new plant part positions based on the current plant parts.
     * New positions are searched relative from each part in all passed directions in random order.
     * A valid position for a new plant part must meet the following requirements:
     * <ul>
     * <li> The position is (a) adjacent to one part or (b) adjacent & one block below and has air above it
     * <li> The block at the new position is air
     * <li> The blow below the new position is not air
     * <li> The block below the new position has the required soil category for the part
     * </ul>
     * @param parts Position of the current plant parts/blocks
     * @param searchDirections Directions to search for, relative to each part
     * @param requiredSoilCategory The soil category, required for the ground below each part. Can be null to accept all kind of soils.
     * @return The new position for a plant part on a valid position or null if no valid position was found.
     */
    private Vector3i searchNewPartPosition(List<Vector3i> parts, List<Direction> searchDirections, String requiredSoilCategory) {
        List<Vector3i> partsCopy = new ArrayList<>(parts);
        Collections.shuffle(partsCopy);
        List<Direction> directionsCopy = new ArrayList<>(searchDirections);
        Collections.shuffle(directionsCopy);

        for (Vector3i part : partsCopy) {
            for (Direction direction : directionsCopy) {
                Vector3i possiblePosition = new Vector3i(part).add(direction.getVector3i());
                //we are only interested in positions where the blocks are air
                if (isAirBlock(possiblePosition)) {
                    Vector3i oneDown = new Vector3i(possiblePosition).subY(1);
                    if (!isAirBlock(oneDown)) {
                        //one down is no air, so check the soil type next
                        if (requiredSoilCategory == null || hasBlockSoilCategory(oneDown, requiredSoilCategory)) {
                            //air at the position and valid soil type one down -> grow in this direction
                            return possiblePosition;
                        }
                    } else {
                        //one block down is air
                        Vector3i twoDown = new Vector3i(possiblePosition).subY(2);
                        if (!isAirBlock(twoDown) && (requiredSoilCategory == null || hasBlockSoilCategory(twoDown, requiredSoilCategory))) {
                            //two down is no air and has the required soil category -> grow at the block one down
                            //(crawling down shallow hills)
                            return oneDown;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isAirBlock(Vector3i blockPosition) {
        return worldProvider.getBlock(blockPosition) == airBlock;
    }

    private boolean hasBlockSoilCategory(Vector3i blockPosition, String soilCategory) {
        return worldProvider.getBlock(blockPosition).getBlockFamily().hasCategory(soilCategory);
    }
}
