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

import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.GrowthStage;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
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

import java.util.Map;
import java.util.Set;

/**
 * System managing the lifecycle of bushes.
 * <p>
 * See {@link BushDefinitionComponent} for an explanation of the lifecycle.  This system also
 * manages vine buds, which are similar to bushes in many respects.  See
 * {@link org.terasology.simpleFarming.components.VineDefinitionComponent} for an explanation of
 * the vine lifecycle.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class BushAuthoritySystem extends BaseComponentSystem {

    /** Maximum single-axis impulse for seed and produce drops. */
    private static final float DROP_IMPULSE_AMOUNT = 22.0f;

    @In private WorldProvider worldProvider;
    @In private BlockManager blockManager;
    @In private InventoryManager inventoryManager;
    @In private BlockEntityRegistry blockEntityRegistry;
    @In private EntityManager entityManager;
    @In private DelayManager delayManager;

    private FastRandom random = new FastRandom();

    /**
     * Called when each {@code BushDefinitionComponent} is first added to an entity.
     * <p>
     * Handles construction of {@link BushDefinitionComponent#stages} from
     * {@link BushDefinitionComponent#growthStages}.
     *
     * @param event         the addition event
     * @param bush          the bush entity
     * @param bushComponent the bush component being added
     */
    @ReceiveEvent
    public void onBushComponentAdded(OnActivatedComponent event, EntityRef bush, BushDefinitionComponent bushComponent) {
        bushComponent.stages = buildGrowthStages(bushComponent.growthStages);
        bush.saveComponent(bushComponent);
    }

    /**
     * Builds the list of growth stages from the prefab data.
     *
     * @param growthStages the prefab GrowthStage data
     * @return the array of growth stages
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
     * Called immediately after a bush seed has been planted.
     * <p>
     * Sets the bush's position and initial growth stage and starts the timer for its next growth
     * event (according to the {@link GrowthStage#minTime} and {@link GrowthStage#maxTime} values
     * for this growth stage).  When the timer expires,
     * {@link #onBushGrowth(DelayedActionTriggeredEvent, EntityRef, BushDefinitionComponent)}
     * will be called.
     *
     * @param event         the seed planting event
     * @param bush          the newly-created bush entity
     * @param bushComponent the bush's definition
     * @see PlantAuthoritySystem#onSeedPlant(ActivateEvent, EntityRef, SeedDefinitionComponent)
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
     * Called when the bush or vine bud should grow.
     * <p>
     * Updates the current growth stage, and then resets the timer, as appropriate to the new stage.
     *
     * @param event         the event indicating the timer has ended
     * @param bush          the bush to grow
     * @param bushComponent the bush's definition
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
     * Grows a bush or vine bud by the specified number of stages.
     * <p>
     * If {@code stages} is negative, this can "un-grow" the bush.
     *
     * @param bushComponent the definition of the bush being grown
     * @param stages        the number of stages to grow; negative values represent un-growth
     */
    private EntityRef doBushGrowth(BushDefinitionComponent bushComponent, int stages) {
        bushComponent.currentStage += stages;
        worldProvider.setBlock(bushComponent.position, bushComponent.stages[bushComponent.currentStage].block);
        EntityRef newBush = blockEntityRegistry.getBlockEntityAt(bushComponent.position);
        newBush.addOrSaveComponent(bushComponent);
        return newBush;
    }

    /**
     * Called when an attempt to harvest the bush is made.
     * <p>
     * Drops produce as appropriate, and then resets or destroys the bush, as indicated by
     * the bush's {@link BushDefinitionComponent#sustainable sustainable} value.
     *
     * @param event  the activation event
     * @param entity the entity doing the harvesting
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
     * Checks if the entities involved in a harvest event are valid.
     * <p>
     * The entities are valid if they both exist, and if the target is a bush or vine bud (i.e., an
     * entity possessing a {@link BushDefinitionComponent}).
     *
     * @param target    the entity being harvested
     * @param harvester the entity doing the harvesting
     * @return true if they are valid, false otherwise
     */
    private boolean areValidHarvestEntities(EntityRef target, EntityRef harvester) {
        return target.exists() && harvester.exists()
            && target.hasComponent(BushDefinitionComponent.class);
    }

    /**
     * Called when a bush or vine bud has been destroyed.
     * <p>
     * Delegates to {@link #onPlantDestroyed(DoDestroyPlant, EntityRef, BushDefinitionComponent)}
     * via a {@link DoDestroyPlant} event.
     *
     * @param event  the block destruction event
     * @param entity the bush or vine bud being destroyed
     */
    @ReceiveEvent
    public void onBushDestroyed(CreateBlockDropsEvent event, EntityRef entity, BushDefinitionComponent bushComponent) {
        entity.send(new DoDestroyPlant());
        event.consume();
    }

    /**
     * Called when a bush or bud is destroyed.
     * <p>
     * Delegates to either {@link #onBushDestroyed(BushDefinitionComponent)} or
     * {@link #onBudDestroyed(BushDefinitionComponent, boolean)} as appropriate.
     *
     * @param event         the destroy plant event
     * @param entity        the entity sending the event; not used
     * @param bushComponent the bush component on the plant
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
     * Handles dropping the correct seeds when a bush (not a vine bud) is destroyed.
     *
     * @param bushComponent the bush component of the entity
     */
    private void onBushDestroyed(BushDefinitionComponent bushComponent) {
        if (bushComponent.currentStage == bushComponent.stages.length - 1) {
            dropSeeds(random.nextInt(1, 3),
                    bushComponent.seed == null ? bushComponent.produce : bushComponent.seed,
                    bushComponent.position.toVector3f());
        }
    }

    /**
     * Handles dropping the correct seeds and notifying the vine when a bud is destroyed.
     *
     * @param bushComponent the component of the bud
     * @param isParentDead  whether the parent vine still exists
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
     * Drops a number of seeds at the position.
     *
     * @param numSeeds the number of seeds to drop
     * @param seed     the prefab of the seed entity
     * @param position the position to drop above
     */
    private void dropSeeds(int numSeeds, String seed, Vector3f position) {
        for (int i = 0; i < numSeeds; i++) {
            EntityRef seedItem = entityManager.create(seed);
            seedItem.send(new DropItemEvent(position.add(0, 0.5f, 0)));
            seedItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        }
    }

    /**
     * Creates the produce and gives it to the harvester or drops it.
     *
     * @param produce   the prefab of the produce entity
     * @param position  the position to drop above
     * @param harvester the entity to give the item to
     * @param target    the bush or vine bud (the "giver" of the item)
     */
    private void dropProduce(String produce, Vector3f position, EntityRef harvester, EntityRef target) {
        EntityRef produceItem = entityManager.create(produce);
        boolean giveSuccess = inventoryManager.giveItem(harvester, target, produceItem);
        if (!giveSuccess) {
            produceItem.send(new DropItemEvent(position.add(0, 0.5f, 0)));
            produceItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        }
    }

    /**
     * Checks if a bush is in the last stage of its growth.
     *
     * @param bushComponent the component of the bush entity to check
     * @return true if the bush is in the last stage, false otherwise
     */
    private boolean isInLastStage(BushDefinitionComponent bushComponent) {
        return bushComponent.currentStage == bushComponent.stages.length - 1;
    }

    /**
     * Starts a new growth timer with random duration, subject to the given bounds.
     *
     * @param entity the entity to set the timer on
     * @param min    the minimum duration in milliseconds
     * @param max    the maximum duration in milliseconds
     */
    private void resetDelay(EntityRef entity, int min, int max) {
        delayManager.addDelayedAction(entity, "SimpleFarming:" + entity.getId(), generateRandom(min, max));
    }

    /**
     * Returns a random integer in the specified interval.
     *
     * @param min the minimum number
     * @param max the maximum number
     * @return the random number, or {@code min} if {@code max <= min}
     */
    private long generateRandom(int min, int max) {
        return max <= min ? min : random.nextLong(min, max);
    }
}
