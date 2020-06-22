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
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.logic.common.RetainComponentsComponent;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.simpleFarming.events.BeforePlanted;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

/**
 * System handling the planting of seeds.
 * <p>
 * After the seed has been planted, management of the resulting bush or vine is passed off to
 * {@link BushAuthoritySystem} or {@link VineAuthoritySystem}, as appropriate.
 *
 * @see SeedDefinitionComponent
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class PlantAuthoritySystem extends BaseComponentSystem {

    @In
    private InventoryManager inventoryManager;
    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;
    @In
    private BlockManager blockManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantAuthoritySystem.class);

    /**
     * The standard air block, cached on initialization.
     */
    private Block airBlock;

    private static Random random = new FastRandom();

    @Override
    public void postBegin() {
        super.postBegin();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
    }

    /**
     * Called when a seed is activated, typically by right-clicking on the ground.
     * <p>
     * If the planting is valid (the event was not consumed by a higher priority handler, and the
     * player was targeting the top of a valid block per {@link #isValidPosition}), creates an
     * entity from the {@linkplain SeedDefinitionComponent#prefab prefab} associated with the seed,
     * and sends that entity an {@link OnSeedPlanted} event.  It is then the responsibility of the
     * appropriate authority ({@link BushAuthoritySystem}, {@link VineAuthoritySystem} or
     * {@link TreeAuthoritySystem}) to manage the plant.
     *
     * @param event         the activation event
     * @param seed          the seed item
     * @param seedComponent the seed's definition component
     */
    @ReceiveEvent(priority = EventPriority.PRIORITY_HIGH)
    public void onSeedPlant(ActivateEvent event, EntityRef seed, SeedDefinitionComponent seedComponent) {
        RetainComponentsComponent retainComponentsComponent = new RetainComponentsComponent();
        retainComponentsComponent.components.add(GenomeComponent.class);
        /* The item is being used but not planted */
        if (event.getTargetLocation() == null || event.isConsumed()) {
            return;
        }
        Vector3i position = new Vector3i(event.getTargetLocation()).addY(1);
        if (Side.inDirection(event.getHitNormal()) == Side.TOP && isValidPosition(position)) {
            /* If the prefab field is null, there is a DefinitionComponent on the seed */
            EntityRef plantEntity = seedComponent.prefab == null ? seed : entityManager.create(seedComponent.prefab);
            plantEntity.send(new BeforePlanted(seed));
            plantEntity.send(new OnSeedPlanted(position));
            plantEntity.addOrSaveComponent(retainComponentsComponent);
            LOGGER.info("Seed was planted. genes of the bush are now "+plantEntity.getComponent(GenomeComponent.class).genes);
            inventoryManager.removeItem(seed.getOwner(), seed, seed, true, 1);
            event.consume();
        }
    }

    /**
     * Determines whether a seed can be planted at the given position.
     * <p>
     * A position is considered valid if it is currently an air block, and if the block below it is not
     * {@linkplain Block#isPenetrable() penetrable}.
     *
     * @param position the position the new plant will occupy (should be air currently); null returns false
     * @return true if the position is valid, false otherwise
     */
    private boolean isValidPosition(Vector3i position) {
        if (position == null) {
            return false;
        }
        Block targetBlock = worldProvider.getBlock(position);

        /* Avoid construction of a transient Vector3i in order to save memory */
        position.addY(-1);
        Block belowBlock = worldProvider.getBlock(position);
        position.addY(1);

        return (targetBlock == airBlock && !belowBlock.isPenetrable());
    }

    /**
     * Returns a random integer in the specified interval.
     *
     * @param min The minimum number
     * @param max The maximum number
     * @return the random number, or {@code min} if {@code max <= min}
     */
    public static long generateRandom(int min, int max) {
        return max == 0 ? min : random.nextInt(min, max);
    }
}
