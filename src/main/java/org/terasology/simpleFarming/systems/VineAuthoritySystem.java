// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.systems;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.CheatGrowthComponent;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.simpleFarming.components.VineDefinitionComponent;
import org.terasology.simpleFarming.components.VineNodeComponent;
import org.terasology.simpleFarming.events.DoDestroyPlant;
import org.terasology.simpleFarming.events.DoRemoveBud;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

/**
 * System managing the lifecycle of vines.
 * <p>
 * See {@link VineDefinitionComponent} for an explanation of the vine lifecycle.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class VineAuthoritySystem extends BaseComponentSystem {

    /**
     * The maximum number of non-air blocks adjacent to a vine stem block.
     */
    private static final int MAX_NEIGHBOURS = 2;

    /**
     * The percentage chance that a new bud will spawn each growth cycle.
     * <p>
     * This is checked for each vine stem block that doesn't already have a bud, every growth
     * cycle.
     */
    private static final double BUD_CHANCE = 0.2;

    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private DelayManager delayManager;
    @In
    private EntityManager entityManager;

    private FastRandom random = new FastRandom();

    /**
     * The standard air block, cached on initialization.
     */
    private Block airBlock;

    /**
     * Array holding all possible spawn directions.
     */
    private Vector3i[] spawnPos = new Vector3i[4];

    @Override
    public void postBegin() {
        super.postBegin();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
        spawnPos[0] = new Vector3i(-1, 0, 0);
        spawnPos[1] = new Vector3i(1, 0, 0);
        spawnPos[2] = new Vector3i(0, 0, 1);
        spawnPos[3] = new Vector3i(0, 0, -1);
    }

    /**
     * Called immediately after a new vine has been planted.
     * <p>
     * Places the root block, creates the {@link VineNodeComponent} associated to the root, and
     * starts the growth timer.  When the timer expires,
     * {@link #onVineGrowth(DelayedActionTriggeredEvent, EntityRef, VineNodeComponent, VineDefinitionComponent)}
     * will be called.
     *
     * @param event            the seed planting event
     * @param definitionEntity the newly-created vine entity
     * @param vineComponent    the vine's definition
     * @see PlantAuthoritySystem#onSeedPlant(ActivateEvent, EntityRef, SeedDefinitionComponent)
     */
    @ReceiveEvent
    public void onVinePlanted(OnSeedPlanted event, EntityRef definitionEntity, VineDefinitionComponent vineComponent) {
        worldProvider.setBlock(event.getPosition(), vineComponent.stem);
        EntityRef vine = blockEntityRegistry.getExistingEntityAt(event.getPosition());
        vine.addOrSaveComponent(vineComponent);
        vine.addComponent(new VineNodeComponent(event.getPosition()));
        resetDelay(vine, vineComponent.minGrowTime, vineComponent.maxGrowTime);
    }

    /**
     * Called periodically to grow the current vine.
     * <p>
     * See {@link #recurseGrow(EntityRef, VineDefinitionComponent)} for details of what happens during a growth cycle.
     * After the growth cycle is complete, restarts the growth timer.
     *
     * @param event         the event indicating the timer has ended
     * @param root          the vine root
     * @param nodeComponent The vine's definition
     */
    @ReceiveEvent
    public void onVineGrowth(DelayedActionTriggeredEvent event, EntityRef root, VineNodeComponent nodeComponent, VineDefinitionComponent vineComponent) {
        doGrowVine(root, nodeComponent, vineComponent);
    }

    private void doGrowVine(EntityRef root, VineNodeComponent nodeComponent, VineDefinitionComponent vineComponent) {
        if (nodeComponent.length != -1) {
            if (nodeComponent.length < vineComponent.maxLength) {
                recurseGrow(root, vineComponent);
            }
            resetDelay(root, vineComponent.minGrowTime, vineComponent.maxGrowTime);
        }
    }

    /**
     * Called when an item with the cheat component is used on a block
     * <p>
     * Grows the targeted vine
     *
     * @param event
     * @param item
     * @param cheatGrowthComponent
     * @param itemComponent
     */
    @ReceiveEvent
    public void onCheatGrowth(ActivateEvent event, EntityRef item, CheatGrowthComponent cheatGrowthComponent, ItemComponent itemComponent) {
        EntityRef target = event.getTarget();
        if (!target.hasComponent(VineDefinitionComponent.class) || !target.hasComponent(VineNodeComponent.class)) {
            return;
        }

        VineDefinitionComponent vineDefinitionComponent = target.getComponent(VineDefinitionComponent.class);
        VineNodeComponent vineNodeComponent = target.getComponent(VineNodeComponent.class);
        if (!cheatGrowthComponent.causesUnGrowth) {
            doGrowVine(target, vineNodeComponent, vineDefinitionComponent);
        }
    }


    /**
     * Recursively grows the vine.
     * <p>
     * Called once for each stem block in the vine.  Each stem block that doesn't already have a bud
     * has a chance to grow one, and if the vine is not already at maximum length (20 blocks),
     * a new stem block is added at the end.
     *
     * @param node          the current node to process
     * @param vineComponent the vine's definition
     * @return the {@link VineNodeComponent#length} of this node, after all growth is complete
     */
    private int recurseGrow(EntityRef node, VineDefinitionComponent vineComponent) {
        VineNodeComponent nodeComponent = node.getComponent(VineNodeComponent.class);
        if (nodeComponent.bud != null && nodeComponent.child != null) {
            nodeComponent.length = recurseGrow(nodeComponent.child, vineComponent) + 1;

        } else if (nodeComponent.child != null) {
            if (random.nextDouble() < BUD_CHANCE) {
                if (addBud(node, vineComponent)) {
                    return nodeComponent.length;
                }
            }
            nodeComponent.length = recurseGrow(nodeComponent.child, vineComponent) + 1;
        } else {
            if (addChild(node, vineComponent)) {
                nodeComponent.length = 1;
            }
        }
        node.addOrSaveComponent(nodeComponent);
        return nodeComponent.length;
    }

    /**
     * Attempts to add a new bud to the vine.
     * <p>
     * After the new bud entity has been created, passes control to the {@link BushAuthoritySystem}
     * via an {@link OnSeedPlanted} event.  The {@code BushAuthoritySystem} is responsible for
     * managing the remainder of the bud's lifecycle.
     * <p>
     * This method can fail to add a bud if there are no valid positions adjacent to {@code parent}.
     * See {@link #isValidPosition(Vector3i)} for the definition of a valid position.
     *
     * @param parent the budding vine node
     * @return true if a bud was added, or false if no valid position for a bud could be found
     */
    private boolean addBud(EntityRef parent, VineDefinitionComponent vineComponent) {
        VineNodeComponent nodeComponent = parent.getComponent(VineNodeComponent.class);
        Vector3i pos = getGrowthPosition(nodeComponent, true);
        if (pos != null) {
            EntityRef budEntity = entityManager.create(vineComponent.bud);
            BushDefinitionComponent bushComponent = budEntity.getComponent(BushDefinitionComponent.class);
            bushComponent.parent = parent;
            budEntity.saveComponent(bushComponent);

            budEntity.send(new OnSeedPlanted(pos));

            nodeComponent.bud = budEntity;
            parent.saveComponent(nodeComponent);
            return true;
        }
        return false;
    }


    /**
     * Attempts to grow a new block of vine stem.
     * <p>
     * This method can fail to add a new stem block if there are no valid positions adjacent to
     * {@code parent}.  See {@link #isValidPosition(Vector3i)} for the definition of a valid
     * position.
     *
     * @param parent        the vine node to attach to
     * @param vineComponent the vine's definition
     * @return true if the child was added, or false if no valid position could be found
     */
    private boolean addChild(EntityRef parent, VineDefinitionComponent vineComponent) {
        VineNodeComponent nodeComponent = parent.getComponent(VineNodeComponent.class);
        Vector3i pos = getGrowthPosition(nodeComponent, false);
        if (pos != null) {
            worldProvider.setBlock(pos, vineComponent.stem);
            nodeComponent.child = blockEntityRegistry.getExistingEntityAt(pos);
            nodeComponent.child.addComponent(new VineNodeComponent(parent, pos));
            parent.addOrSaveComponent(nodeComponent);
            return true;
        }
        return false;
    }

    /**
     * Returns the position to spawn a new vine element in.
     * <p>
     * The positions considered are the four positions adjacent to {@code parent} and on the same Y-level.
     * See {@link #isValidPosition(Vector3i)} for the definition of a valid position; additionally, a
     * position is considered invalid if it is horizontally adjacent to more than {@link #MAX_NEIGHBOURS}
     * non-air blocks.
     * <p>
     * If a valid position exists, returns one selected at random; otherwise returns null.
     * <p>
     * Get the position to spawn a new vine element in
     *
     * @param parent the node this new element will be attached to
     * @param isBud  if the node is to be a bud.
     * @return a position to grow in, or null if none exist
     */
    private Vector3i getGrowthPosition(VineNodeComponent parent, boolean isBud) {

        shuffleArray(spawnPos);
        for (Vector3i possiblePos : spawnPos) {
            Vector3i nextPos = new Vector3i(possiblePos);
            nextPos.add(parent.position);

            if ((isValidPosition(nextPos) && (countNeighbours(nextPos) <= MAX_NEIGHBOURS || isBud))) {
                return nextPos;
            }
        }

        return null;
    }

    /**
     * Checks if the position is a valid position for a new stem or bud.
     * <p>
     * A position is considered valid if:
     * <ol>
     * <li>It is currently occupied by an air block.</li>
     * <li>The block immediately beneath it is not {@linkplain Block#isPenetrable()} penetrable.</li>
     * </ol>
     *
     * @param position the position to check for validity; null returns false
     * @return true if a vine can grow there, false otherwise
     */
    private boolean isValidPosition(Vector3i position) {
        if (position == null) {
            return false;
        }
        Block targetBlock = worldProvider.getBlock(position);
        if (targetBlock != airBlock) {
            return false;
        }
        Block belowBlock = worldProvider.getBlock(position.add(0,-1,0));
        position.add(0,1,0);
        if (belowBlock.isPenetrable()) {
            return false;
        }
        return true;
    }

    /**
     * Counts how many non-air blocks are next to the given position.
     * <p>
     * Only blocks in the same x-z plane are considered, and the position itself is disregarded.
     *
     * @param position the position whose neighbours to count
     * @return the number of blocks found
     */
    private int countNeighbours(Vector3i position) {
        int count = 0;
        Vector3i pos = new Vector3i();
        pos.y = position.y();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                pos.x = position.x + x;
                pos.z = position.z + z;
                Block neighbour = worldProvider.getBlock(pos);
                if (!neighbour.equals(airBlock)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Called when a bud is destroyed.
     *
     * @param event  the removal event
     * @param parent the bud's parent node
     * @see BushAuthoritySystem#onBudDestroyed(Vector3ic, EntityRef, BushDefinitionComponent, boolean)
     */
    @ReceiveEvent
    public void onBudRemove(DoRemoveBud event, EntityRef parent, VineNodeComponent nodeComponent) {
        nodeComponent.bud = null;
        parent.saveComponent(nodeComponent);
    }

    /**
     * Called when a vine stem block is destroyed.
     * <p>
     * Destroys all vine blocks (both stems and buds) that are now disconnected from the root.
     *
     * @param event  the block destruction event
     * @param entity the vine stem block being destroyed
     */
    @ReceiveEvent
    public void onVineDestroyed(CreateBlockDropsEvent event, EntityRef entity, VineNodeComponent nodeComponent) {
        recurseKill(entity);
        if (nodeComponent.parent == null) {
            nodeComponent.length = -1;
        } else {
            VineNodeComponent parentNodeComponent = nodeComponent.parent.getComponent(VineNodeComponent.class);
            parentNodeComponent.child.destroy();
            parentNodeComponent.child = null;
            nodeComponent.parent.saveComponent(parentNodeComponent);
            rebuildLength(nodeComponent.parent, 0);
        }
        event.consume();
    }

    /**
     * Invalidate and remove all nodes in a vine from the specified node down.
     *
     * @param node the node to invalidate from
     */
    private void recurseKill(EntityRef node) {
        VineNodeComponent nodeComponent = node.getComponent(VineNodeComponent.class);
        worldProvider.setBlock(nodeComponent.position, airBlock);
        if (nodeComponent.child != null) {
            recurseKill(nodeComponent.child);
        }
        if (nodeComponent.bud != null) {
            nodeComponent.bud.send(new DoDestroyPlant(true));
        }
        node.destroy();
    }

    /**
     * Recursively recalculates the length for each node from the end towards the root.
     *
     * @param node   the node to calculate the length for
     * @param length the length of this node
     */
    private void rebuildLength(EntityRef node, int length) {
        VineNodeComponent nodeComponent = node.getComponent(VineNodeComponent.class);
        nodeComponent.length = length;
        if (nodeComponent.parent != null) {
            rebuildLength(nodeComponent.parent, length + 1);
        }
        node.saveComponent(nodeComponent);
    }

    /**
     * Shuffles an array in place.
     *
     * @param elements the array to shuffle
     */
    private void shuffleArray(Vector3i[] elements) {
        Vector3i temp;
        int a;
        int b;
        for (int i = 0; i < elements.length / 2; i++) {
            a = random.nextInt(elements.length);
            b = random.nextInt(elements.length);
            temp = elements[a];
            elements[a] = elements[b];
            elements[b] = temp;
        }
    }

    /**
     * Starts a new growth timer with random duration, subject to the given bounds.
     *
     * @param entity the entity to set the timer on
     * @param min    the minimum duration in milliseconds
     * @param max    the maximum duration in milliseconds
     */
    private void resetDelay(EntityRef entity, int min, int max) {
        delayManager.addDelayedAction(entity, "SimpleFarming:" + entity.getId(), PlantAuthoritySystem.generateRandom(min, max));
    }
}
