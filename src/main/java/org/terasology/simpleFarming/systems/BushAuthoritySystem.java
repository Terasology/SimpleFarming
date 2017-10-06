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
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.geom.Vector3i;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.GrowthStage;
import org.terasology.simpleFarming.events.DoDestroyPlant;
import org.terasology.simpleFarming.events.DoRemoveBud;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class BushAuthoritySystem extends BaseComponentSystem {
    private static final float DROP_IMPULSE_AMOUNT = 22.0f;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private InventoryManager inventoryManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private EntityManager entityManager;
    @In
    private DelayManager delayManager;

    private FastRandom random = new FastRandom();

    /**
     * Called when a bush seed is planted
     *
     * @param event         The seed planting event
     * @param bush          The bush entity that has just been made
     * @param bushComponent The bush definition component
     */
    @ReceiveEvent
    public void onBushPlanted(OnSeedPlanted event, EntityRef bush, BushDefinitionComponent bushComponent) {
        bushComponent.position = event.getPosition();
        bushComponent.currentStage = -1;

        EntityRef newBush = doBushGrowth(bushComponent, 1);
        resetDelay(newBush, bushComponent.stages[0].minTime, bushComponent.stages[0].maxTime);
        bush.saveComponent(bushComponent);
    }

    /**
     * Called when a bushComponent is added to an entity.
     * Converts the stages to an array
     *
     * @param event         The addition event
     * @param bush          The bush entity
     * @param bushComponent THe bush component being added
     */
    @ReceiveEvent
    public void onBushComponentAdded(OnActivatedComponent event, EntityRef bush, BushDefinitionComponent bushComponent) {
        bushComponent.stages = buildGrowthStages(bushComponent.growthStages);
        bush.saveComponent(bushComponent);
    }

    /**
     * Called when a bush or bud is destroyed.
     * Delegates to the correct handler function in either case
     *
     * @param event         The destroy plant event
     * @param entity        The entity sending
     * @param bushComponent The bush component on the plant
     */
    @ReceiveEvent
    public void onPlantDestroyed(DoDestroyPlant event, EntityRef entity, BushDefinitionComponent bushComponent) {
        if (bushComponent.parent == null) {
            onBushDestroyed(bushComponent);
        } else {
            onBudDestroyed(bushComponent, event.isParentDead);
        }
    }

    /**
     * Handles dropping the correct seeds when a bush is destroyed
     *
     * @param bushComponent The bush component of the entity
     */
    private void onBushDestroyed(BushDefinitionComponent bushComponent) {
        if (bushComponent.currentStage == bushComponent.stages.length - 1) {
            dropSeeds(random.nextInt(1, 3),
                    bushComponent.seed == null ? bushComponent.produce : bushComponent.seed,
                    bushComponent.position.toVector3f());
        }
    }

    /**
     * Handles dropping the correct seeds & notifying the vine when a bud is destroyed
     *
     * @param bushComponent The component of the bud
     * @param isParentDead  Whether the parent vine still exists
     */
    private void onBudDestroyed(BushDefinitionComponent bushComponent, boolean isParentDead) {
        if (!isParentDead) {
            bushComponent.parent.send(new DoRemoveBud());
        }
        worldProvider.setBlock(bushComponent.position, blockManager.getBlock(BlockManager.AIR_ID));
        dropSeeds(1,
                bushComponent.seed == null ? bushComponent.produce : bushComponent.seed,
                bushComponent.position.toVector3f());

    }

    /**
     * Called when the bush should grow
     *
     * @param event         The event indicating the timer has ended
     * @param bush          The bush to grow
     * @param bushComponent The bush's definition
     */
    @ReceiveEvent
    public void onBushGrowth(DelayedActionTriggeredEvent event, EntityRef bush, BushDefinitionComponent bushComponent) {
        EntityRef newBush = null;
        if (!isInLastStage(bushComponent)) {
            newBush = doBushGrowth(bushComponent, 1);
        }
        resetDelay(newBush == null ? bush : newBush,
                bushComponent.stages[bushComponent.currentStage].minTime,
                bushComponent.stages[bushComponent.currentStage].maxTime);
    }

    /**
     * Called when an attempt to harvest the bush is made.
     * If the plant is actually a bud then a link back to the vine is made.
     *
     * @param event  The activation event
     * @param entity The entity doing the harvesting
     */
    @ReceiveEvent
    public void onHarvest(ActivateEvent event, EntityRef entity) {
        EntityRef target = event.getTarget();
        EntityRef harvester = entity.equals(target) ? event.getInstigator() : entity;
        if (!event.isConsumed() && areValidHarvestEntities(target, harvester)) {

            BushDefinitionComponent bushComponent = target.getComponent(BushDefinitionComponent.class);
            /* Produce is only given in the final stage */
            if (isInLastStage(bushComponent)) {
                dropProduce(bushComponent.produce, event.getTargetLocation(), harvester, target);
                if (bushComponent.sustainable) {
                    resetDelay(doBushGrowth(bushComponent, -1),
                    bushComponent.stages[bushComponent.currentStage].minTime,
                            bushComponent.stages[bushComponent.currentStage].maxTime);
                } else {
                    entity.send(new DoDestroyPlant());
                    worldProvider.setBlock(bushComponent.position, blockManager.getBlock(BlockManager.AIR_ID));
                }
                event.consume();
            }
        }
    }

    /**
     * Handles the seed drop on plant destroyed event.
     *
     * @param event  The event corresponding to the plant destroy.
     * @param entity Reference to the plant entity.
     */
    @ReceiveEvent
    public void onBushDestroyed(CreateBlockDropsEvent event, EntityRef entity, BushDefinitionComponent bushComponent) {
        entity.send(new DoDestroyPlant());
        event.consume();
    }

    /**
     * Drops a number of seeds at the position
     *
     * @param numSeeds The amount of seeds to drop
     * @param seed     The prefab of the entity to drop
     * @param position The position to drop above.
     */
    private void dropSeeds(int numSeeds, Prefab seed, Vector3f position) {
        for (int i = 0; i < numSeeds; i++) {
            EntityRef seedItem = entityManager.create(seed);
            seedItem.send(new DropItemEvent(position.add(0, 0.5f, 0)));
            seedItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        }
    }

    /**
     * Creates the produce and gives it to the harvester or drops it
     *
     * @param produce   The produce to create
     * @param position  The position to drop at
     * @param harvester The entity to give the item to
     * @param target    The entity to take the item from
     */
    private void dropProduce(Prefab produce, Vector3f position, EntityRef harvester, EntityRef target) {
        EntityRef produceItem = entityManager.create(produce);
        boolean giveSuccess = inventoryManager.giveItem(harvester, target, produceItem);
        if (!giveSuccess) {
            produceItem.send(new DropItemEvent(position.add(0, 0.5f, 0)));
            produceItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        }
    }

    /**
     * Grow the bush by one stage in a specified direction
     *
     * @param bushComponent The definition of the bush to grow
     * @param direction     The direction to grow in. positive indicates forward & negative indicated backwards
     */
    private EntityRef doBushGrowth(BushDefinitionComponent bushComponent, int direction) {
        bushComponent.currentStage += direction;
        worldProvider.setBlock(bushComponent.position, bushComponent.stages[bushComponent.currentStage].block);
        EntityRef newBush = blockEntityRegistry.getBlockEntityAt(bushComponent.position);
        newBush.addOrSaveComponent(bushComponent);
        return newBush;
    }


    /**
     * Builds the list of growth stages from the prefab data
     *
     * @param growthStages The prefab GrowthStage data
     * @return The array of growth stages
     */
    private GrowthStage[] buildGrowthStages(Map<String, GrowthStage> growthStages) {
        Set<Map.Entry<String, GrowthStage>> entrySet = growthStages.entrySet();
        GrowthStage[] stages = new GrowthStage[entrySet.size()];
        int i = 0;
        for (Map.Entry<String, GrowthStage> entry : entrySet) {
            stages[i] = new GrowthStage(entry.getValue());
            stages[i].block = blockManager.getBlock(entry.getKey());
            stages[i].block.setKeepActive(true);
            i++;
        }
        return stages;
    }

    /**
     * Check if the entities in the harvest are valid
     *
     * @param target    The entity being harvested
     * @param harvester The entity harvesting
     * @return True if they are valid, false otherwise
     */
    private boolean areValidHarvestEntities(EntityRef target, EntityRef harvester) {
        return target.exists() && harvester.exists()
                && target.hasComponent(BushDefinitionComponent.class);
    }

    /**
     * Checks if a bush is in the last stage of growth
     *
     * @param bushComponent The component of the bush entity to check
     * @return True if the bush is in the last stage, false otherwise
     */
    private boolean isInLastStage(BushDefinitionComponent bushComponent) {
        return bushComponent.currentStage == bushComponent.stages.length - 1;
    }

    /**
     * Adds a new delay of random length between the items
     *
     * @param entity The entity to have the timer set on
     * @param min    The minimum duration in milliseconds
     * @param max    THe maximum duration in milliseconds
     */
    private void resetDelay(EntityRef entity, int min, int max) {
        delayManager.addDelayedAction(entity, "SimpleFarming:" + entity.getId(), generateRandom(min, max));
    }

    /**
     * Creates a random number between the minimum and the maximum.
     *
     * @param min The minimum number
     * @param max The maximum number
     * @return The random number or min if max <= min
     */
    private long generateRandom(int min, int max) {
        return max <= min ? min : random.nextLong(min, max);
    }
}
