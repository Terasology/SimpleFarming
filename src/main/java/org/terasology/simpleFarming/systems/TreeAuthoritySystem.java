package org.terasology.simpleFarming.systems;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RegisterSystem(RegisterMode.AUTHORITY)
public class TreeAuthoritySystem extends BaseComponentSystem {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(TreeAuthoritySystem.class);

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
	
	@ReceiveEvent
	public void onTreePlanted(OnSeedPlanted event, EntityRef definitionEntity, SaplingDefinitionComponent saplingComponent) {
		worldProvider.setBlock(event.getPosition(), saplingComponent.sapling);
		saplingComponent.location = event.getPosition();
		EntityRef sapling = blockEntityRegistry.getExistingEntityAt(event.getPosition());
        sapling.addOrSaveComponent(saplingComponent);
        TreeGrowthStage currentStage = saplingComponent.growthStages.get(0);
        resetDelay(sapling, currentStage.minTime, currentStage.maxTime);
	}
	
	@ReceiveEvent
    public void onSaplingGrowth(DelayedActionTriggeredEvent event, EntityRef sapling, SaplingDefinitionComponent saplingComponent) {
		growSapling(saplingComponent);
	}
	
	@ReceiveEvent
	public void onRootGrowth(DelayedActionTriggeredEvent event, EntityRef rootEntity, LogComponent logComponent, RootComponent rootComponent) {
		if(!rootComponent.alive || rootComponent.growthStage+1 == rootComponent.growthStages.size()) {
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
	
	@ReceiveEvent
	public void onSaplingDestroyed(CreateBlockDropsEvent event, EntityRef sapling, SaplingDefinitionComponent saplingComponent) {
		String seed = saplingComponent.leaf.getComponent(BushDefinitionComponent.class).seed;
		EntityRef seedItem = entityManager.create(seed);
        seedItem.send(new DropItemEvent(saplingComponent.location.toVector3f().add(0, 0.5f, 0)));
        seedItem.send(new ImpulseEvent(random.nextVector3f(DROP_IMPULSE_AMOUNT)));
        
        event.consume();
	}
	
	@ReceiveEvent
	public void onLogDestroyed(DoDestroyEvent event, EntityRef log, LogComponent logComponent) {
		destroyLog(event, log);
	}
	
	private boolean canGenerateTree(EntityRef rootEntity) {
		LogComponent logComponent = rootEntity.getComponent(LogComponent.class);
		RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
		TreeGrowthStage currentStage = rootComponent.growthStages.get(rootComponent.growthStage);
		Vector3i location = new Vector3i(logComponent.location).addY(1);
		int rootY = location.y;
		for(; location.y-rootY < currentStage.height-1; location.addY(1)) {
			if(!isValidBlock(location, rootEntity)) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidBlock(Vector3i position, EntityRef rootEntity) {
		if(position == null) {
			return false;
		}
		
		EntityRef e = blockEntityRegistry.getExistingEntityAt(position);
		if(e != EntityRef.NULL && e.hasComponent(LogComponent.class)
				&& e.getComponent(LogComponent.class).root == rootEntity) {
			return true;
		}
		if(rootEntity.getComponent(RootComponent.class).leaves.contains(e)) {
			return true;
		}
		
		Block b = worldProvider.getBlock(position);
		return b == airBlock;
	}

	private boolean isValidBlock(Vector3i position) {
		if(position == null) {
			return false;
		}
		
		Block b = worldProvider.getBlock(position);
		return b == airBlock;
	}

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
	
	private void generateTree(EntityRef rootEntity) {
		LogComponent logComponent = rootEntity.getComponent(LogComponent.class);
		RootComponent rootComponent = rootEntity.getComponent(RootComponent.class);
		
		Block log = rootComponent.log;
		Prefab leaf = rootComponent.leaf;
		
		TreeGrowthStage currentStage = rootComponent.growthStages.get(rootComponent.growthStage);
		Vector3i location = new Vector3i(logComponent.location).addY(1);
		int rootY = location.y;
		for(; location.y-rootY < currentStage.height-1; location.addY(1)) {
			addLog(new Vector3i(location), log, false, rootEntity);
		}
		
		Set<Vector3i> leaves = currentStage.getLeaves();
		for(Vector3i leafLocation : leaves) {
			addLeaf(new Vector3i(logComponent.location).add(leafLocation), leaf, rootComponent);
		}
		
		rootComponent.alive = true;
		rootEntity.addOrSaveComponent(rootComponent);
	}
	
	private EntityRef addLog(Vector3i location, Block log, boolean force) {
		if(force || isValidBlock(location)) {
			worldProvider.setBlock(location, log);
			return blockEntityRegistry.getExistingEntityAt(location);
		}
		return EntityRef.NULL;
	}
	
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
	
	private EntityRef addLog(Vector3i location, Block log, boolean force, SaplingDefinitionComponent base) {
		EntityRef logEntity = addLog(location, log, force, EntityRef.NULL);
		if(logEntity != null && logEntity != EntityRef.NULL) {
			RootComponent rootComponent = new RootComponent(base);
			logEntity.addOrSaveComponent(rootComponent);
		}
		return logEntity;
	}
	
	private EntityRef addLeaf(Vector3i location, Prefab leaf) {
		if(isValidBlock(location)) {
			EntityRef leafEntity = entityManager.create(leaf);
			leafEntity.send(new OnSeedPlanted(location));
			return blockEntityRegistry.getExistingEntityAt(location);
		}
		return EntityRef.NULL;
	}
	
	private EntityRef addLeaf(Vector3i location, Prefab leaf, RootComponent rootComponent) {
		EntityRef leafEntity = addLeaf(location, leaf);
		if(leafEntity != null && leafEntity != EntityRef.NULL) {
			rootComponent.leaves.add(leafEntity);
		}
		return leafEntity;
	}
	
	private void destroyTree(EntityRef rootEntity, Vector3i location) {
		location.addY(1);
		EntityRef aboveLogEntity = blockEntityRegistry.getExistingEntityAt(location);
		location.addY(-1);
		if(aboveLogEntity != EntityRef.NULL && aboveLogEntity.hasComponent(LogComponent.class)
				&& aboveLogEntity.getComponent(LogComponent.class).root == rootEntity) {
			destroyLog(null, aboveLogEntity);
		}
	}

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
    	String actionId = "SimpleFarming:" + entity.getId();
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
}
