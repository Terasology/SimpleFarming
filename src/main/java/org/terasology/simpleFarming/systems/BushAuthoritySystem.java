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

import org.joml.Vector3ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.common.RetainComponentsComponent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.BushGrowthStage;
import org.terasology.simpleFarming.components.CheatGrowthComponent;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.simpleFarming.events.DoDestroyPlant;
import org.terasology.simpleFarming.events.DoRemoveBud;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.simpleFarming.events.ProduceCreated;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * System managing the lifecycle of bushes.
 * <p>
 * See {@link BushDefinitionComponent} for an explanation of the lifecycle.  This system also manages vine buds, which
 * are similar to bushes in many respects.  See {@link org.terasology.simpleFarming.components.VineDefinitionComponent}
 * for an explanation of the vine lifecycle.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class BushAuthoritySystem extends BaseComponentSystem {

    /**
     * Maximum single-axis impulse for seed and produce drops.
     */
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BushAuthoritySystem.class);

    /**
     * Builds the list of growth stages from the prefab data.
     *
     * @param growthStages the prefab GrowthStage data
     * @return the array of growth stages
     */
    private BushGrowthStage[] buildGrowthStages(Map<String, BushGrowthStage> growthStages) {
        Set<Map.Entry<String, BushGrowthStage>> entrySet = growthStages.entrySet();
        BushGrowthStage[] stages = new BushGrowthStage[entrySet.size()];
        int i = 0;
        for (Map.Entry<String, BushGrowthStage> entry : entrySet) {
            stages[i] = new BushGrowthStage(entry.getValue());
            stages[i].block = blockManager.getBlock(entry.getKey());
            stages[i].block.setKeepActive(true);
            i++;
        }
        return stages;
    }

    /**
     * Called immediately after a bush seed has been planted.
     * <p>
     * Sets the bush's position and initial growth stage and starts the timer for its next growth event (according to
     * the {@link BushGrowthStage#minTime} and {@link BushGrowthStage#maxTime} values for this growth stage).  When the
     * timer expires, {@link #onBushGrowth(DelayedActionTriggeredEvent, EntityRef, BushDefinitionComponent,
     * BlockComponent)} will be called.
     *
     * @param event the seed planting event
     * @param bush the newly-created bush entity
     * @param bushComponent the bush's definition
     * @see PlantAuthoritySystem#onSeedPlant(ActivateEvent, EntityRef, SeedDefinitionComponent)
     */
    @ReceiveEvent
    public void onBushPlanted(OnSeedPlanted event, EntityRef bush, BushDefinitionComponent bushComponent) {
//        bush.send(new BeforeSeedPlanted());
        bushComponent.currentStage = -1;
        doBushGrowth(event.getPosition(), bush, bushComponent, 1);
        bush.saveComponent(bushComponent);
    }

    /**
     * Called when the bush or vine bud should grow.
     * <p>
     * Updates the current growth stage, and then resets the timer, as appropriate to the new stage.
     *
     * @param event the event indicating the timer has ended
     * @param bush the bush to grow
     * @param bushComponent the bush's definition
     */
    @ReceiveEvent
    public void onBushGrowth(DelayedActionTriggeredEvent event, EntityRef bush, BushDefinitionComponent bushComponent
            , BlockComponent blockComponent) {
        doBushGrowth(blockComponent.getPosition(), bush, bushComponent, 1);
    }


    /**
     * Called when an item with the cheat component is used on a block
     * <p>
     * Grows or "un-grow" the targeted bush
     *
     * @param event
     * @param item
     * @param cheatGrowthComponent
     * @param itemComponent
     */
    @ReceiveEvent
    public void onCheatGrowth(ActivateEvent event, EntityRef item, CheatGrowthComponent cheatGrowthComponent,
                              ItemComponent itemComponent) {
        EntityRef target = event.getTarget();
        if (!areValidHarvestEntities(target, event.getInstigator())) {
            return;
        }

        BlockComponent blockComponent = target.getComponent(BlockComponent.class);
        BushDefinitionComponent bushDefinitionComponent = target.getComponent(BushDefinitionComponent.class);
        if (cheatGrowthComponent.causesUnGrowth) {
            doBushGrowth(blockComponent.getPosition(), target, bushDefinitionComponent, -1);
        } else {
            doBushGrowth(blockComponent.getPosition(), target, bushDefinitionComponent, 1);
        }
    }

    /**
     * Grows a bush or vine bud by the specified number of stages.
     * <p>
     * If {@code stages} is negative, this can "un-grow" the bush.
     *
     * @param bushComponent the definition of the bush being grown
     * @param stages the number of stages to grow; negative values represent un-growth
     */
    private void doBushGrowth(Vector3i position, EntityRef bush, BushDefinitionComponent bushComponent, int stages) {
        if (!isInLastStage(bushComponent)
                // allow negative growth from the last stage
                || stages < 0) {
            RetainComponentsComponent retainComponentsComponent = new RetainComponentsComponent();
            retainComponentsComponent.components.add(GenomeComponent.class);
            bush.addOrSaveComponent(retainComponentsComponent);
            bushComponent.currentStage += stages;
            Map.Entry<String, BushGrowthStage> stage = getGrowthStage(bushComponent, bushComponent.currentStage);
            worldProvider.setBlock(position, blockManager.getBlock(stage.getKey()));
            EntityRef newBush = blockEntityRegistry.getBlockEntityAt(position);
            newBush.addOrSaveComponent(retainComponentsComponent);
            newBush.addOrSaveComponent(bushComponent);

            if (stage.getValue().maxTime > 0 && stage.getValue().minTime > 0) {
                resetDelay(newBush,
                        stage.getValue().minTime,
                        stage.getValue().maxTime);
            }
            newBush.addOrSaveComponent(retainComponentsComponent);

            if (bush.hasComponent(GenomeComponent.class)) {
                newBush.addOrSaveComponent(retainComponentsComponent);
                newBush.addOrSaveComponent(bush.getComponent(GenomeComponent.class));
                LOGGER.info("on growth this the genes " + bush.getComponent(GenomeComponent.class).genes);
                LOGGER.info("for checking growth, this the newBush genes " + newBush.getComponent(GenomeComponent.class).genes);
            }
        }
    }

    /**
     * Safely get the growth stage from the given index
     *
     * @param bushComponent
     * @param index
     * @return
     */
    public static Map.Entry<String, BushGrowthStage> getGrowthStage(BushDefinitionComponent bushComponent, int index) {
        return (new ArrayList<>(bushComponent.growthStages.entrySet())).get(Math.min(bushComponent.growthStages.size() - 1, Math.max(0, index)));
    }

    /**
     * Called when an attempt to harvest the bush is made.
     * <p>
     * Drops produce as appropriate, and then resets or destroys the bush, as indicated by the bush's {@link
     * BushDefinitionComponent#sustainable sustainable} value.
     *
     * @param event the activation event
     * @param entity the block entity
     */
    @ReceiveEvent
    public void onHarvest(ActivateEvent event, EntityRef entity, BushDefinitionComponent bushComponent,
                          BlockComponent blockComponent) {
        RetainComponentsComponent retainComponentsComponent = new RetainComponentsComponent();
        retainComponentsComponent.components.add(GenomeComponent.class);
        entity.addOrSaveComponent(retainComponentsComponent);
        EntityRef harvester = event.getInstigator();
        //remove this block
        if (entity.hasComponent(GenomeComponent.class)) {
            LOGGER.info("Activate event was called on" + entity.getId() + "this stage has genome comp");
        } else {
            LOGGER.info("Activate event was called on" + entity.getId() + "no genome comp");
        }
        if (!event.isConsumed() && areValidHarvestEntities(entity, harvester)) {
            /* Produce is only given in the final stage */
            if (isInLastStage(bushComponent)) {
                EntityRef produceItem = dropProduce(bushComponent.produce, event.getTargetLocation(), harvester,
                        entity);
                //remove this if needed and remove dependency if not needed
                if (entity.hasComponent(GenomeComponent.class)) {
                    LOGGER.info("Has genes before!");
                    LOGGER.info(entity.getComponent(GenomeComponent.class).genes);
                }
                entity.send(new ProduceCreated(entity, produceItem));
                //remove this if needed and remove dependency if not needed
                if (entity.hasComponent(GenomeComponent.class)) {
                    LOGGER.info("Has genes after!");
                    LOGGER.info(entity.getComponent(GenomeComponent.class).genes);
                    LOGGER.info("entity is " + entity.getParentPrefab().getName());
                }
                if (bushComponent.sustainable) {
                    doBushGrowth(blockComponent.getPosition(), entity, bushComponent, -1);
                } else {
                    entity.send(new DoDestroyPlant());
                    worldProvider.setBlock(blockComponent.getPosition(), blockManager.getBlock(BlockManager.AIR_ID));
                    entity.destroy();
                }
                event.consume();
            }
        }
    }

    /**
     * Checks if the entities involved in a harvest event are valid.
     * <p>
     * The entities are valid if they both exist, and if the target is a bush or vine bud (i.e., an entity possessing a
     * {@link BushDefinitionComponent}) and a {@link BlockComponent}.
     *
     * @param target the entity being harvested
     * @param harvester the entity doing the harvesting
     * @return true if they are valid, false otherwise
     */
    private boolean areValidHarvestEntities(EntityRef target, EntityRef harvester) {
        return target.exists() && harvester.exists()
                && target.hasComponent(BushDefinitionComponent.class)
                && target.hasComponent(BlockComponent.class);
    }

    /**
     * Called when a bush or vine bud has been destroyed.
     * <p>
     * Delegates to {@link #onPlantDestroyed(DoDestroyPlant, EntityRef, BushDefinitionComponent, BlockComponent)} via a
     * {@link DoDestroyPlant} event.
     *
     * @param event the block destruction event
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
     * Delegates to either {@link #onBushDestroyed(Vector3i, BushDefinitionComponent)} or {@link
     * #onBudDestroyed(Vector3i, BushDefinitionComponent, boolean)} as appropriate.
     *
     * @param event the destroy plant event
     * @param entity the entity sending the event; not used
     * @param bushComponent the bush component on the plant
     */
    @ReceiveEvent
    public void onPlantDestroyed(DoDestroyPlant event, EntityRef entity, BushDefinitionComponent bushComponent,
                                 BlockComponent blockComponent) {
        if (bushComponent.parent == null) {
            onBushDestroyed(blockComponent.getPosition(), bushComponent);
        } else {
            onBudDestroyed(blockComponent.getPosition(), bushComponent, event.isParentDead);
        }
    }

    /**
     * Handles dropping the correct seeds when a bush (not a vine bud) is destroyed.
     *
     * @param bushComponent the bush component of the entity
     */
    private void onBushDestroyed(Vector3i position, BushDefinitionComponent bushComponent) {
        if (bushComponent.currentStage == bushComponent.growthStages.size() - 1) {
            dropSeeds(numSeeds(bushComponent),
                    bushComponent.seed == null ? bushComponent.produce : bushComponent.seed,
                    position.toVector3f(), bushComponent);
            //seedItem.send(new ProduceCreated(bushComponent.parent,seedItem));
        }
    }

    /**
     * Handles dropping the correct seeds and notifying the vine when a bud is destroyed.
     *
     * @param bushComponent the component of the bud
     * @param isParentDead whether the parent vine still exists
     */
    private void onBudDestroyed(Vector3i position, BushDefinitionComponent bushComponent, boolean isParentDead) {
        if (!isParentDead) {
            bushComponent.parent.send(new DoRemoveBud());
        }
        worldProvider.setBlock(position, blockManager.getBlock(BlockManager.AIR_ID));
        dropSeeds(1,
                bushComponent.seed == null ? bushComponent.produce : bushComponent.seed,
                position.toVector3f(), bushComponent);

    }

    /**
     * Drops a number of seeds at the position.
     *
     * @param numSeeds the number of seeds to drop
     * @param seed the prefab of the seed entity
     * @param position the position to drop above
     */
    private void dropSeeds(int numSeeds, String seed, Vector3f position, BushDefinitionComponent bushComponent) {
        for (int i = 0; i < numSeeds; i++) {
            EntityRef seedItem = entityManager.create(seed);
            seedItem.send(new DropItemEvent(position.add(0, 0.5f, 0)));
            seedItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
            seedItem.send(new ProduceCreated(bushComponent.parent, seedItem));
        }
    }

    /**
     * Generates a random number of seeds to drop for a bush.
     *
     * @param bushComponent The bush definition whose {@link BushDefinitionComponent#seedDropChances} will be
     *         used
     * @return A randomly generated number of seeds to drop
     */
    private int numSeeds(BushDefinitionComponent bushComponent) {
        int sum = 0;
        for (int weight : bushComponent.seedDropChances) {
            sum += weight;
        }

        int rand = random.nextInt(sum);

        for (int i = 0; i < bushComponent.seedDropChances.size(); i++) {
            int weight = bushComponent.seedDropChances.get(i);
            if (rand < weight) {
                return i;
            }
            rand -= weight;
        }
        return 0;
    }

    /**
     * Creates the produce and gives it to the harvester or drops it.
     *
     * @param produce the prefab of the produce entity
     * @param position the position to drop above
     * @param harvester the entity to give the item to
     * @param target the bush or vine bud (the "giver" of the item)
     */
    private EntityRef dropProduce(String produce, Vector3f position, EntityRef harvester, EntityRef target) {
        EntityRef produceItem = entityManager.create(produce);
        boolean giveSuccess = inventoryManager.giveItem(harvester, target, produceItem);
        if (!giveSuccess) {
            produceItem.send(new DropItemEvent(position.add(0, 0.5f, 0)));
            produceItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        }
        return produceItem;
    }

    /**
     * Checks if a bush is in the last stage of its growth.
     *
     * @param bushComponent the component of the bush entity to check
     * @return true if the bush is in the last stage, false otherwise
     */
    private boolean isInLastStage(BushDefinitionComponent bushComponent) {
        return bushComponent.currentStage == bushComponent.growthStages.size() - 1;
    }

    /**
     * Starts a new growth timer with random duration, subject to the given bounds.
     *
     * @param entity the entity to set the timer on
     * @param min the minimum duration in milliseconds
     * @param max the maximum duration in milliseconds
     */
    private void resetDelay(EntityRef entity, int min, int max) {
        delayManager.addDelayedAction(entity, "SimpleFarming:" + entity.getId(),
                PlantAuthoritySystem.generateRandom(min, max));
    }
}
