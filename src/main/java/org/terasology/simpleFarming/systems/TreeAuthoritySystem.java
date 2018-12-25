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

import java.util.HashSet;
import java.util.Set;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.CheatGrowthComponent;
import org.terasology.simpleFarming.components.LogComponent;
import org.terasology.simpleFarming.components.RootComponent;
import org.terasology.simpleFarming.components.SaplingDefinitionComponent;
import org.terasology.simpleFarming.components.TreeGrowthStage;
import org.terasology.simpleFarming.events.OnSeedPlanted;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.CreateBlockDropsEvent;

/**
 * Manages the growth, destruction, and other events for trees.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class TreeAuthoritySystem extends BaseComponentSystem {
	/**
     * Maximum single-axis impulse for seed and produce drops.
     */
    private static final float DROP_IMPULSE_AMOUNT = 22.0f;

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
	
	private Block airBlock;
	
	@Override
	public void postBegin() {
		super.postBegin();
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);
	}
	
	/**
	 * Creates a sapling and adds the SaplingDefinitionComponent to it, then resets the growth timer using {@link #resetDelay(EntityRef, int, int)}.
	 * 
	 * @param event The event for the seed being planted.
	 * @param definitionEntity The entity receiving the event, not used.
	 * @param saplingComponent The component defining the sapling.
	 * 
	 * @see PlantAuthoritySystem
	 */
	@ReceiveEvent
	public void onTreePlanted(OnSeedPlanted event, EntityRef definitionEntity, SaplingDefinitionComponent saplingComponent) {
		worldProvider.setBlock(event.getPosition(), saplingComponent.sapling);
		saplingComponent.location = event.getPosition();
		EntityRef sapling = blockEntityRegistry.getExistingEntityAt(event.getPosition());
        sapling.addOrSaveComponent(saplingComponent);
        TreeGrowthStage currentStage = saplingComponent.growthStages.get(0);
        resetDelay(sapling, currentStage.minTime, currentStage.maxTime);
	}
	
	/**
	 * Grows the sapling into a tree using {@link #growSapling(SaplingDefinitionComponent)}.
	 * 
	 * @param event The delayed growth event.
	 * @param sapling The sapling's block entity in the world, not used.
	 * @param saplingComponent The sapling definition used to grow the tree.
	 */
	@ReceiveEvent
    public void onSaplingGrowth(DelayedActionTriggeredEvent event, EntityRef sapling, SaplingDefinitionComponent saplingComponent) {
	    if(event.getActionId().equals("SimpleFarming:" + sapling.getId() + ":Growth")) {
		    growSapling(saplingComponent);
	    }
	}
	
	/**
	 * Grows a tree based on a growth timer. This is done by first checking if the tree can be grown, then destroying the old one and growing a
	 * new tree with the next growth stage. After that, the growth timer is reset if there is another stage.
	 * 
	 * @param event The delayed growth event.
	 * @param rootEntity The block entity of the lowest log block in the tree, aka the "root".
	 * @param logComponent The log component of the entity.
	 * @param rootComponent The root component of the entity.
	 * 
	 * @see #canGenerateTree(EntityRef)
	 * @see #destroyTree(EntityRef, Vector3i)
	 * @see #generateTree(EntityRef)
	 * @see #resetDelay(EntityRef, int, int)
	 */
	@ReceiveEvent
	public void onRootGrowth(DelayedActionTriggeredEvent event, EntityRef rootEntity, LogComponent logComponent, RootComponent rootComponent) {
	    if(!rootComponent.alive || rootComponent.growthStage+1 == rootComponent.growthStages.size()
		        || !event.getActionId().equals("SimpleFarming:" + rootEntity.getId() + ":Growth")) {
			return;
		}
		
		rootComponent.growthStage++;
		if(canGenerateTree(rootEntity)) {
			rootEntity.addOrSaveComponent(rootComponent);
			destroyTree(rootEntity, logComponent.location);
			generateTree(rootEntity);
			if(rootComponent.growthStage+1 < rootComponent.growthStages.size()) {
				TreeGrowthStage nextStage = rootComponent.growthStages.get(rootComponent.growthStage+1);
				resetDelay(rootEntity, nextStage.minTime, nextStage.maxTime);
			}
		}
	}
	
	/**
	 * Reacts to an item with a CheatGrowthComponent being used on a sapling or log block, and appropriately grows or un-grows the tree.
	 * 
	 * @param event The event from right-clicking with an item.
	 * @param item The item that was used.
	 * @param cheatGrowthComponent The CheatGrowthComponent of that item.
	 * @param itemComponent The ItemComponent of that item, not used.
	 * 
	 * @see #growSapling(SaplingDefinitionComponent)
	 * @see #onRootGrowth(DelayedActionTriggeredEvent, EntityRef, LogComponent, RootComponent)
	 */
	@ReceiveEvent
	public void onCheatGrowth(ActivateEvent event, EntityRef item, CheatGrowthComponent cheatGrowthComponent, ItemComponent itemComponent) {
		EntityRef target = event.getTarget();
		if(target.hasComponent(SaplingDefinitionComponent.class)) {
			if(!cheatGrowthComponent.causesUnGrowth) {
				growSapling(target.getComponent(SaplingDefinitionComponent.class));
			}
		} else if(target.hasComponent(LogComponent.class)) {
			EntityRef rootEntity = target.getComponent(LogComponent.class).root;
			RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
			if(!rootComponent.alive) {
				return;
			}
			
			LogComponent logComponent = rootEntity.getComponent(LogComponent.class);
			if(cheatGrowthComponent.causesUnGrowth) {
				if(rootComponent.growthStage == 0) {
					destroyTree(rootEntity, logComponent.location);
					worldProvider.setBlock(logComponent.location, rootComponent.sapling);
					EntityRef saplingEntity = blockEntityRegistry.getExistingEntityAt(logComponent.location);
					SaplingDefinitionComponent saplingComponent = new SaplingDefinitionComponent(rootComponent, logComponent.location);
					saplingEntity.addOrSaveComponent(saplingComponent);
					TreeGrowthStage currentStage = saplingComponent.growthStages.get(0);
			        resetDelay(saplingEntity, currentStage.minTime, currentStage.maxTime);
					return;
				}
				rootComponent.growthStage--;
				rootEntity.addOrSaveComponent(rootComponent);
				destroyTree(rootEntity, logComponent.location);
				generateTree(rootEntity);
				TreeGrowthStage nextStage = rootComponent.growthStages.get(rootComponent.growthStage+1);
				resetDelay(rootEntity, nextStage.minTime, nextStage.maxTime);
			} else {
				if(rootComponent.growthStage+1 == rootComponent.growthStages.size()) {
					return;
				}
				
				rootComponent.growthStage++;
				if(canGenerateTree(rootEntity)) {
					rootEntity.addOrSaveComponent(rootComponent);
					destroyTree(rootEntity, logComponent.location);
					generateTree(rootEntity);
					if(rootComponent.growthStage+1 < rootComponent.growthStages.size()) {
						TreeGrowthStage nextStage = rootComponent.growthStages.get(rootComponent.growthStage+1);
						resetDelay(rootEntity, nextStage.minTime, nextStage.maxTime);
					}
				}
			}
		}
	}
	
	/**
	 * Ensures that the sapling item is dropped when a sapling is destroyed, and not the block itself.
	 * The sapling item is taken from the {@link BushDefinitionComponent#seed} of the {@link SaplingDefinitionComponent#leaf}
	 * prefab.
	 * 
	 * @param event The event for items being dropped from the block.
	 * @param sapling The sapling's block entity.
	 * @param saplingComponent The sapling's definition.
	 */
	@ReceiveEvent
	public void onSaplingDestroyed(CreateBlockDropsEvent event, EntityRef sapling, SaplingDefinitionComponent saplingComponent) {
		String seed = saplingComponent.leaf.getComponent(BushDefinitionComponent.class).seed;
		EntityRef seedItem = entityManager.create(seed);
        seedItem.send(new DropItemEvent(saplingComponent.location.toVector3f().add(0, 0.5f, 0)));
        seedItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        
        event.consume();
	}
	
	/**
	 * Destroys a log block and all of the logs above it, along with all of the leaves on the tree, then marks the root as dead.
	 * This is done by calling {@link #destroyLog(DoDestroyEvent, EntityRef)}.
	 * 
	 * @param event The event for a log being destroyed.
	 * @param log The block entity of the log which got destroyed.
	 * @param logComponent The log's LogComponent
	 */
	@ReceiveEvent
	public void onLogDestroyed(DoDestroyEvent event, EntityRef log, LogComponent logComponent) {
		destroyLog(event, log);
	}
	
	/**
	 * Checks if a root is able to generate the tree for its current growth stage. It is able to generate if nothing is blocking
	 * the positions where new log blocks would go. Because this method is also used for checking if a currently existing tree can be
	 * grown, it will consider logs and leaves which are part of the same tree to be valid spaces.
	 * 
	 * @param rootEntity The block entity for the root of the tree.
	 * 
	 * @return Whether or not the tree can be generated.
	 * 
	 * @see #isValidBlock(Vector3i, EntityRef)
	 */
	private boolean canGenerateTree(EntityRef rootEntity) {
		LogComponent logComponent = rootEntity.getComponent(LogComponent.class);
		RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
		TreeGrowthStage currentStage = rootComponent.growthStages.get(rootComponent.growthStage);
		Vector3i location = new Vector3i(logComponent.location).addY(1);
		int rootY = location.y;
		while(location.y-rootY < currentStage.height-1) {
			if(!isValidBlock(location, rootEntity)) {
				return false;
			}
			location.addY(1);
		}
		return true;
	}

	/**
	 * Checks if the position is a valid space to spawn a new block. The space is considered valid if it is an air block or if it is part
	 * of the tree.
	 * 
	 * @param position The position to check.
	 * @param rootEntity The block entity of the root of the tree this block will be part of, used for checking if existing blocks are part
	 * of the same tree.
	 * 
	 * @return Whether or not the position is valid.
	 */
	private boolean isValidBlock(Vector3i position, EntityRef rootEntity) {
		if(position == null) {
			return false;
		}
		
		EntityRef e = blockEntityRegistry.getExistingEntityAt(position);
		return isValidBlock(position) //Air block
		        || (e != EntityRef.NULL && e.hasComponent(LogComponent.class) && e.getComponent(LogComponent.class).root == rootEntity)   //Log Block
		        || rootEntity.getComponent(RootComponent.class).leaves.contains(e);   //Leaf block
	}

	/**
	 * Checks whether the given position is a valid space to spawn a block. This method considers only air blocks to be valid spaces.
	 * 
	 * @param position The position to check.
	 * 
	 * @return Whether or not the position is valid.
	 */
	private boolean isValidBlock(Vector3i position) {
		if(position == null) {
			return false;
		}
		
		Block b = worldProvider.getBlock(position);
		return b == airBlock;
	}

	/**
	 * Grows a sapling into a tree by first creating a root, then generating the tree if it can grow, otherwise reverting back to the
	 * sapling form.
	 * 
	 * @param saplingComponent The sapling's definition.
	 * 
	 * @see #addLog(Vector3i, Block, boolean, SaplingDefinitionComponent)
	 * @see #canGenerateTree(EntityRef)
	 * @see #generateTree(EntityRef)
	 */
	private void growSapling(SaplingDefinitionComponent saplingComponent) {
		Vector3i location = saplingComponent.location;
		Block log = saplingComponent.log;
		
		EntityRef rootEntity = addLog(location, log, true, saplingComponent);
		if(canGenerateTree(rootEntity)) {
			generateTree(rootEntity);
			RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
			if(rootComponent.growthStage+1 < rootComponent.growthStages.size()) {
				TreeGrowthStage nextStage = rootComponent.growthStages.get(rootComponent.growthStage+1);
				resetDelay(rootEntity, nextStage.minTime, nextStage.maxTime);
			}
		} else {
			worldProvider.setBlock(location, saplingComponent.sapling);
			EntityRef saplingEntity = blockEntityRegistry.getExistingEntityAt(location);
			saplingEntity.addOrSaveComponent(saplingComponent);
			TreeGrowthStage currentStage = saplingComponent.growthStages.get(0);
	        resetDelay(saplingEntity, currentStage.minTime, currentStage.maxTime);
			return;
		}
	}
	
	/**
	 * Creates a tree using the root's current growth stage.
	 * 
	 * @param rootEntity The block entity for the root.
	 * 
	 * @see TreeGrowthStage
	 * @see #addLog(Vector3i, Block, boolean, EntityRef)
	 * @see #addLeaf(Vector3i, Prefab, RootComponent)
	 */
	private void generateTree(EntityRef rootEntity) {
		LogComponent logComponent = rootEntity.getComponent(LogComponent.class);
		RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
		
		Block log = rootComponent.log;
		Prefab leaf = rootComponent.leaf;
		
		TreeGrowthStage currentStage = rootComponent.growthStages.get(rootComponent.growthStage);
		Vector3i location = new Vector3i(logComponent.location).addY(1);
		int rootY = location.y;
		while(location.y-rootY < currentStage.height-1) {
			addLog(new Vector3i(location), log, false, rootEntity);
            location.addY(1);
		}
		
		Set<Vector3i> leaves = getLeaves(currentStage);
		for(Vector3i leafLocation : leaves) {
			addLeaf(new Vector3i(logComponent.location).add(leafLocation), leaf, rootComponent);
		}
		
		rootComponent.alive = true;
		rootEntity.addOrSaveComponent(rootComponent);
	}
	
	/**
	 * Creates a log block. This method does not add any components, it is mainly a helper method.
	 * 
	 * @param location The place to add the log.
	 * @param log The block to use for the log.
	 * @param force Whether or not the log should be forced into its position, ignoring its validity.
	 * 
	 * @return The newly created log's block entity.
	 * 
	 * @see #addLog(Vector3i, Block, boolean, EntityRef)
	 */
	private EntityRef addLog(Vector3i location, Block log, boolean force) {
		if(force || isValidBlock(location)) {
			worldProvider.setBlock(location, log);
			return blockEntityRegistry.getExistingEntityAt(location);
		}
		return EntityRef.NULL;
	}
	
	/**
	 * Creates a log block and adds a {@link LogComponent} to it.
	 * 
	 * @param location The place to add the log.
	 * @param log The block to use for the log.
	 * @param force Whether or not the log should be forced into its position, ignoring its validity.
	 * @param root The root entity of the current tree, stored in the LogComponent. If it is {@link EntityRef.NULL}, this log will be
	 * set as its own root.
	 * 
	 * @return The newly created log's block entity.
	 * 
	 * @see #addLog(Vector3i, Block, boolean)
	 */
	private EntityRef addLog(Vector3i location, Block log, boolean force, EntityRef root) {
		EntityRef logEntity = addLog(location, log, force);
		if(logEntity != null && logEntity != EntityRef.NULL) {
			LogComponent logComponent = new LogComponent();
			
			logComponent.location = location;
			if(root == EntityRef.NULL) {
				logComponent.root = logEntity;
			} else {
				logComponent.root = root;
			}
			
			logEntity.addOrSaveComponent(logComponent);
		}
		return logEntity;
	}
	
	/**
	 * Creates a new log and gives it a {@link LogComponent} and a {@link RootComponent}.
	 * 
	 * @param location The place to add the log.
	 * @param log The block to use for the log.
	 * @param force Whether or not the log should be forced into its position, ignoring its validity.
	 * @param base The sapling definition to base this root off of.
	 * 
	 * @return The newly created log's block entity.
	 * 
	 * @see #addLog(Vector3i, Block, boolean, EntityRef)
	 */
	private EntityRef addLog(Vector3i location, Block log, boolean force, SaplingDefinitionComponent base) {
		EntityRef logEntity = addLog(location, log, force, EntityRef.NULL);
		if(logEntity != null && logEntity != EntityRef.NULL) {
			RootComponent rootComponent = new RootComponent(base);
			logEntity.addOrSaveComponent(rootComponent);
		}
		return logEntity;
	}
	
	/**
	 * Creates a leaf at the specified location. This method only creates the bush block, it is mainly a helper method.
	 * 
	 * @param location The place to add the leaf block.
	 * @param leaf The prefab for the leaf. It should have a {@link BushDefinitionComponent}
	 * 
	 * @return The newly created leaf's block entity.
	 * 
	 * @see #addLeaf(Vector3i, Prefab, RootComponent)
	 */
	private EntityRef addLeaf(Vector3i location, Prefab leaf) {
		if(isValidBlock(location)) {
			EntityRef leafEntity = entityManager.create(leaf);
			leafEntity.send(new OnSeedPlanted(location));
			return blockEntityRegistry.getExistingEntityAt(location);
		}
		return EntityRef.NULL;
	}
	
	/**
	 * Creates a leaf at the specified location and adds it to a tree using its root.
	 * 
	 * @param location The place to add the leaf block.
	 * @param leaf The prefab for the leaf. It should have a {@link BushDefinitionComponent}
	 * @param rootComponent The RootComponent to add the leaf's {@link EntityRef} to.
	 * 
	 * @return The newly created leaf's block entity.
	 * 
	 * @see #addLeaf(Vector3i, Prefab)
	 * @see RootComponent#leaves
	 */
	private EntityRef addLeaf(Vector3i location, Prefab leaf, RootComponent rootComponent) {
		EntityRef leafEntity = addLeaf(location, leaf);
		if(leafEntity != null && leafEntity != EntityRef.NULL) {
			rootComponent.leaves.add(leafEntity);
		}
		return leafEntity;
	}
	
	/**
	 * Destroys all of the blocks in a tree except for the root. This is done usually to make way for a new growth stage of the tree.
	 * 
	 * @param rootEntity The root of the tree.
	 * @param location The root's location.
	 */
	private void destroyTree(EntityRef rootEntity, Vector3i location) {
		location.addY(1);
		EntityRef aboveLogEntity = blockEntityRegistry.getExistingEntityAt(location);
		location.addY(-1);
		if(aboveLogEntity != EntityRef.NULL && aboveLogEntity.hasComponent(LogComponent.class)
				&& aboveLogEntity.getComponent(LogComponent.class).root == rootEntity) {
			destroyLog(null, aboveLogEntity);
		}
	}

	/**
	 * Destroys a log block in a tree, and all of the logs above it along with all of the leaves on a tree. Also marks the tree as
	 * no longer alive so that it does not grow anymore.
	 * 
	 * @param event The event for the log being destroyed. If it is present, it will be sent to all other blocks which are destroyed,
	 * allowing them to drop items, otherwise, the blocks will simply be replaced with air, allowing for growth or un-growth.
	 * @param log The log to be destroyed.
	 * 
	 * @see #destroyLeaves(Set, DoDestroyEvent)
	 */
	private void destroyLog(DoDestroyEvent event, EntityRef log) {
		LogComponent logComponent = log.getComponent(LogComponent.class);
		EntityRef rootEntity = logComponent.root;
		RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
		
		if(rootComponent.alive) {
			destroyLeaves(rootComponent.leaves, event);
			rootComponent.leaves.clear();
			rootComponent.alive = false;
			rootEntity.addOrSaveComponent(rootComponent);
		}
		
		if(event == null) {
			worldProvider.setBlock(logComponent.location, airBlock);
		}
		
		Vector3i aboveLocation = new Vector3i(logComponent.location).addY(1);
		EntityRef aboveLogEntity = blockEntityRegistry.getExistingEntityAt(aboveLocation);
		if(aboveLogEntity != EntityRef.NULL && aboveLogEntity.hasComponent(LogComponent.class)
				&& aboveLogEntity.getComponent(LogComponent.class).root == rootEntity) {
			if(event == null) {
				destroyLog(null, aboveLogEntity);
			} else {
				aboveLogEntity.send(event);
			}
		}
	}

	/**
	 * Destroys all of the leaves in a set of leaves.
	 * 
	 * @param leaves The set of leaves to destroy.
	 * @param event The event for the leaves being destroyed. If it is present, it will be sent to all blocks which are destroyed,
	 * allowing them to drop items, otherwise, the blocks will simply be replaced with air, allowing for growth or un-growth.
	 * 
	 * @see RootComponent#leaves
	 */
	private void destroyLeaves(Set<EntityRef> leaves, DoDestroyEvent event) {
		for(EntityRef leaf : leaves) {
			if(leaf.exists() && leaf.hasComponent(BushDefinitionComponent.class)) {
				Vector3i leafLocation = leaf.getComponent(BlockComponent.class).position;
				if(event != null) {
					leaf.send(event);
				} else {
					worldProvider.setBlock(leafLocation, airBlock);
				}
			}
		}
	}

	/**
     * Starts a new growth timer with random duration, subject to the given bounds.
     * If a growth timer had already been started, the previously scheduled event will be removed.
     *
     * @param entity the entity to set the timer on
     * @param min    the minimum duration in milliseconds
     * @param max    the maximum duration in milliseconds
     */
    private void resetDelay(EntityRef entity, int min, int max) {
    	String actionId = "SimpleFarming:" + entity.getId() + ":Growth";
    	if(delayManager.hasDelayedAction(entity, actionId)) {
    		delayManager.cancelDelayedAction(entity, actionId);
    	}
        delayManager.addDelayedAction(entity, actionId, generateRandom(min, max));
    }

    /**
     * Returns a random integer in the specified interval.
     *
     * @param min The minimum number
     * @param max The maximum number
     * @return the random number, or {@code min} if {@code max <= min}
     */
    private long generateRandom(int min, int max) {
        return max == 0 ? min : random.nextInt(min, max);
    }

    /**
     * Gets the leaf locations for a growth stage.
     * If using a default configuration, the leaves will automatically be adjusted to the {@link TreeGrowthStage#height}
     * 
     * @param growthStage The growth stage to get the values from.
     * 
     * @return A Set of all of the locations where a leaf should be grown.
     */
    private static Set<Vector3i> getLeaves(TreeGrowthStage growthStage) {
    	if(growthStage.defLeaves == 0) {
    		return growthStage.leaves;
    	}
    	
    	Vector3i location = new Vector3i(0, growthStage.height-1, 0);
    	Set<Vector3i> defLeafSet = new HashSet<>();
    	switch(growthStage.defLeaves) {
    		case 3:
    			defLeafSet.add(new Vector3i(0, 2, 0).add(location));
    			defLeafSet.add(new Vector3i(1, 2, 0).add(location));
    			defLeafSet.add(new Vector3i(-1, 2, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 2, 1).add(location));
    			defLeafSet.add(new Vector3i(0, 2, -1).add(location));
    			defLeafSet.add(new Vector3i(1, 2, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, 2, 1).add(location));
    			defLeafSet.add(new Vector3i(1, 2, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, 2, -1).add(location));
    
    			defLeafSet.add(new Vector3i(2, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(2, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(2, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(2, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(2, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(2, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(2, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(2, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(2, -1, -1).add(location));
    
    			defLeafSet.add(new Vector3i(-2, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(-2, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(-2, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(-2, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(-2, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(-2, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(-2, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(-2, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(-2, -1, -1).add(location));
    
    			defLeafSet.add(new Vector3i(1, 1, 2).add(location));
    			defLeafSet.add(new Vector3i(0, 1, 2).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, 2).add(location));
    			defLeafSet.add(new Vector3i(1, 0, 2).add(location));
    			defLeafSet.add(new Vector3i(0, 0, 2).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, 2).add(location));
    			defLeafSet.add(new Vector3i(1, -1, 2).add(location));
    			defLeafSet.add(new Vector3i(0, -1, 2).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, 2).add(location));
    
    			defLeafSet.add(new Vector3i(1, 1, -2).add(location));
    			defLeafSet.add(new Vector3i(0, 1, -2).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, -2).add(location));
    			defLeafSet.add(new Vector3i(1, 0, -2).add(location));
    			defLeafSet.add(new Vector3i(0, 0, -2).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, -2).add(location));
    			defLeafSet.add(new Vector3i(1, -1, -2).add(location));
    			defLeafSet.add(new Vector3i(0, -1, -2).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, -2).add(location));
    		case 2:
    			defLeafSet.add(new Vector3i(-1, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(1, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(1, 1, 1).add(location));
    			
    			defLeafSet.add(new Vector3i(1, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 1, -1).add(location));
    
    			
    			defLeafSet.add(new Vector3i(-1, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(1, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(1, 0, 1).add(location));
    
    			
    			defLeafSet.add(new Vector3i(-1, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(1, -1, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, -1).add(location));
    			defLeafSet.add(new Vector3i(1, -1, 1).add(location));
    			
    			defLeafSet.add(new Vector3i(1, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, -1, -1).add(location));
    		case 1:
    			defLeafSet.add(new Vector3i(0, 1, 0).add(location));
    			
    			defLeafSet.add(new Vector3i(1, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 0, -1).add(location));
    			break;
    	}
    	return defLeafSet;
    }
}
