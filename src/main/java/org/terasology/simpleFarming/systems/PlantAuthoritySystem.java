// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.systems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.math.Side;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.utilities.random.Random;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.inventory.logic.InventoryManager;
import org.terasology.math.geom.Vector3i;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.simpleFarming.events.BeforePlanted;
import org.terasology.simpleFarming.events.OnSeedPlanted;

/**
 * System handling the planting of seeds.
 * <p>
 * After the seed has been planted, management of the resulting bush or vine is passed off to {@link
 * BushAuthoritySystem} or {@link VineAuthoritySystem}, as appropriate.
 *
 * @see SeedDefinitionComponent
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class PlantAuthoritySystem extends BaseComponentSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantAuthoritySystem.class);
    private static final Random random = new FastRandom();
    @In
    private InventoryManager inventoryManager;
    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;
    @In
    private BlockManager blockManager;
    /**
     * The standard air block, cached on initialization.
     */
    private Block airBlock;

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

    @Override
    public void postBegin() {
        super.postBegin();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
    }

    /**
     * Called when a seed is activated, typically by right-clicking on the ground.
     * <p>
     * If the planting is valid (the event was not consumed by a higher priority handler, and the player was targeting
     * the top of a valid block per {@link #isValidPosition}), creates an entity from the {@linkplain
     * SeedDefinitionComponent#prefab prefab} associated with the seed, and sends that entity an {@link OnSeedPlanted}
     * event.  It is then the responsibility of the appropriate authority ({@link BushAuthoritySystem}, {@link
     * VineAuthoritySystem} or {@link TreeAuthoritySystem}) to manage the plant.
     *
     * @param event the activation event
     * @param seed the seed item
     * @param seedComponent the seed's definition component
     */
    @ReceiveEvent(priority = EventPriority.PRIORITY_HIGH)
    public void onSeedPlant(ActivateEvent event, EntityRef seed, SeedDefinitionComponent seedComponent) {
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
            inventoryManager.removeItem(seed.getOwner(), seed, seed, true, 1);
            event.consume();
        }
    }

    /**
     * Determines whether a seed can be planted at the given position.
     * <p>
     * A position is considered valid if it is currently an air block, and if the block below it is not {@linkplain
     * Block#isPenetrable() penetrable}.
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
}
