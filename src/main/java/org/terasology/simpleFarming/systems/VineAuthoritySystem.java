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
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.VineNode;
import org.terasology.simpleFarming.events.DoDestroyPlant;
import org.terasology.simpleFarming.events.DoRemoveBud;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.simpleFarming.components.VineDefinitionComponent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

import java.util.HashMap;
import java.util.Map;


@RegisterSystem(RegisterMode.AUTHORITY)
public class VineAuthoritySystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(VineAuthoritySystem.class);
    private static int maxNeighbours = 2;
    private static double budChance = 0.2;

    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private DelayManager delayManager;
    @In
    private EntityManager entityManager;

    private EntityRef sendingEntity;

    private FastRandom random;
    private Block airBlock;
    private Vector3i[] spawnPos = new Vector3i[4];
    /**
     * Links position to entities as a method of getting the correct entity for that position.
     * This is as block.setEntity() sets for all instances of that block.
     */
    private Map<Vector3f, VineNode> blockEntities = new HashMap<>();

    @Override
    public void postBegin() {
        super.postBegin();
        random = new FastRandom();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
        spawnPos[0] = new Vector3i(-1, 0, 0);
        spawnPos[1] = new Vector3i(1, 0, 0);
        spawnPos[2] = new Vector3i(0, 0, 1);
        spawnPos[3] = new Vector3i(0, 0, -1);
    }


    /**
     * Called when a new vine is to be planted.
     * Handles placing the block, setting up the first node and delegates the periodic growth call
     *
     * @param event         The event containing the position
     * @param vine          The vine entity
     * @param vineComponent The vine's definition
     */
    @ReceiveEvent
    public void onVinePlanted(OnSeedPlanted event, EntityRef vine, VineDefinitionComponent vineComponent) {
        sendingEntity = vine;
        worldProvider.setBlock(event.getPosition(), vineComponent.stem);
        vineComponent.rootNode = new VineNode(event.getPosition());
        vineComponent.rootNode.root = true;
        vine.saveComponent(vineComponent);
        blockEntities.put(event.getPosition().toVector3f(), vineComponent.rootNode);
        resetDelay(vine, vineComponent.minGrowTime, vineComponent.maxGrowTime);
    }

    /**
     * Called regularly to grow the current vine.
     *
     * @param event         The event called periodically
     * @param vine          The vine entity
     * @param vineComponent The vine's definition
     */
    @ReceiveEvent
    public void onVineGrowth(DelayedActionTriggeredEvent event, EntityRef vine, VineDefinitionComponent vineComponent) {
        if (vineComponent.rootNode.height != -1) {
            if (vineComponent.rootNode.height < 20) {
                recurseGrow(vineComponent.rootNode, vineComponent);
            }
            resetDelay(vine, vineComponent.minGrowTime, vineComponent.maxGrowTime);
        }
    }

    /**
     * Recursively grows the vine.
     * This first checks if the node already has a bud.
     * If it does and it also has a child and we recurse onto it.
     * <p>
     * If it does have a child but no bud then there is a chance for it to grow a bud.
     * If we aren't growing a bud then we recurse onto the child
     * <p>
     * If we don't have a child then add one.
     *
     * @param node          The current node to process
     * @param vineComponent The vine's definition
     * @return True if we were able to grow something. Else false
     */
    private int recurseGrow(VineNode node, VineDefinitionComponent vineComponent) {
        if (node.bud != null && node.child != null) {
            node.height = recurseGrow(node.child, vineComponent) + 1;
        } else if (node.child != null) {
            if (random.nextDouble() < budChance) {
                if (addBud(node, vineComponent)) {
                    return node.height;
                }
            }
            node.height = recurseGrow(node.child, vineComponent) + 1;
        } else {
            if (addChild(node, vineComponent)) {
                node.height = 1;
            }
        }
        return node.height;
    }

    /**
     * Add a new bud to the vine
     *
     * @param parent The node to add the bud off
     * @return True if we could add a bud, false otherwise
     */
    private boolean addBud(VineNode parent, VineDefinitionComponent vineComponent) {
        Vector3i pos = getGrowthPosition(parent, true);
        if (pos != null) {
            EntityRef budEntity = entityManager.create(vineComponent.bud);
            BushDefinitionComponent bushComponent = budEntity.getComponent(BushDefinitionComponent.class);
            bushComponent.parentPosition = parent.position;
            budEntity.send(new OnSeedPlanted(pos));
            parent.bud = pos;
            return true;
        }
        return false;
    }


    /**
     * Grows a new child vine section
     *
     * @param parent        The vine node to attach to
     * @param vineComponent The definition of the vine
     * @return True if the child was added, false otherwise.
     */
    private boolean addChild(VineNode parent, VineDefinitionComponent vineComponent) {
        Vector3i pos = getGrowthPosition(parent, false);
        if (pos != null) {
            parent.child = new VineNode(pos);
            parent.child.parent = parent;
            worldProvider.setBlock(pos, vineComponent.stem);
            blockEntities.put(pos.toVector3f(), parent.child);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the position to spawn a new vine element in
     *
     * @param parent The node this new element will be attached to
     * @param isBud  If the node is to be a bud.
     * @return A position to grow in or null if none found.
     */
    private Vector3i getGrowthPosition(VineNode parent, boolean isBud) {
        int i = 0;
        Vector3i nextPos;
        shuffleArray(spawnPos);
        do {
            nextPos = new Vector3i(spawnPos[i]);
            nextPos.add(parent.position);
            i++;
        }
        while ((!validatePosition(nextPos) || (countNeighbours(nextPos) > maxNeighbours && !isBud)) && i < spawnPos.length);
        if (i == spawnPos.length) {
            return null;
        } else {
            return nextPos;
        }
    }

    /**
     * Called when a bud is destroyed
     * @param event
     * @param entity
     */
    @ReceiveEvent
    public void onBudRemove(DoRemoveBud event, EntityRef entity) {
        if (blockEntities.containsKey(event.location.toVector3f())) {
            VineNode node = blockEntities.get(event.location.toVector3f());
            node.bud = null;
        }
    }

    /**
     * Handles the seed drop on plant destroyed event.
     *
     * @param event  The event corresponding to the plant destroy.
     * @param entity Reference to the plant entity.
     */
    @ReceiveEvent
    public void onVineDestroyed(CreateBlockDropsEvent event, EntityRef entity, LocationComponent location) {
        if (blockEntities.containsKey(location.getWorldPosition())) {
            VineNode node = blockEntities.get(location.getWorldPosition());
            recurseKill(node);
            if (!node.root) {
                node.parent.child = null;
                rebuildHeight(node.parent, 0);
            } else {
                node.height = -1;
            }
            event.consume();
        }
    }


    private void recurseKill(VineNode node) {
        worldProvider.setBlock(node.position, airBlock);
        if (node.child != null) {
            recurseKill(node.child);
            node.child = null;
        }
        if (node.bud != null) {
            sendingEntity.send(new DoDestroyPlant(node.bud));
        }
    }

    private void rebuildHeight(VineNode node, int height) {
        node.height = height;
        if (!node.root) {
            rebuildHeight(node.parent, height + 1);
        }
    }


    /**
     * Checks if the position is a valid position to grow in.
     *
     * @param position The position to check for
     * @return True if a vine can grow there. False otherwise
     */
    private boolean validatePosition(Vector3i position) {
        if (position == null) {
            return false;
        }
        Block targetBlock = worldProvider.getBlock(position);
        if (targetBlock != airBlock) {
            return false;
        }
        targetBlock = worldProvider.getBlock(new Vector3i(position.x(), position.y() - 1, position.z()));
        if (targetBlock.isPenetrable()) {
            return false;
        }
        return true;
    }

    /**
     * Counts how many non air blocks are next to the position.
     * Only counts in the x-z plane and includes the position itself
     *
     * @param position The position to check at
     * @return The number of blocks found.
     */
    private int countNeighbours(Vector3i position) {
        int count = 0;
        Vector3i pos = new Vector3i();
        pos.y = position.y();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                pos.x = position.x + x;
                pos.z = position.z + z;
                if (worldProvider.getBlock(pos) != airBlock) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Adds a new delay of random length between the growths
     *
     * @param entity The entity to have the timer set on
     * @param min    The minimum duration
     * @param max    THe maximum duration
     */
    private void resetDelay(EntityRef entity, int min, int max) {
        delayManager.addDelayedAction(entity, "EdibleFlora:" + entity.getId(), generateRandom(min, max));
    }


    /**
     * Shuffles an array in place.
     *
     * @param elements The array to shuffle
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
     * Creates a random number between the minimum and the maximum.
     *
     * @param min The minimum duration
     * @param max The maximum duration
     * @return The random number or min if max <= min
     */
    private long generateRandom(int min, int max) {
        return max == 0 ? min : random.nextInt(min, max);
    }


}
