// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.testing;

import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.destruction.DestroyEvent;
import org.terasology.engine.logic.destruction.EngineDamageTypes;
import org.terasology.engine.logic.inventory.events.DropItemEvent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.logic.players.PlayerCharacterComponent;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.moduletestingenvironment.TestEventReceiver;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.systems.BushAuthoritySystem;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test environment specific to the {@code SimpleFarming} module.
 * <p>
 * Each test run in this environment has access to a single bush, based on a test-specific prefab. The methods on this
 * class can be used to manipulate and make assertions about this bush.
 */
public class SingleBushTestingEnvironment extends ModuleTestingEnvironment {
    private static final Vector3i BUSH_LOCATION = Vector3i.zero();
    private static final String BUSH_SEED_PREFAB = "SimpleFarming:TestSeed";

    private EntityManager entityManager;
    private WorldProvider worldProvider;

    private Block air;
    private Block dirt;

    private EntityRef bush;
    private BushDefinitionComponent component;

    private static void assertListContainsEntityFromPrefab(List<EntityRef> items, String prefab) {
        assertTrue(String.format("Operation did not drop %s", prefab),
                items.stream().anyMatch(item -> entityIsFromPrefab(item, prefab)));
    }

    private static boolean entityIsFromPrefab(EntityRef entity, String prefab) {
        return entity.getParentPrefab().getName().equals(prefab);
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.singleton("SimpleFarming");
    }

    @Override
    public void setup() throws Exception {
        super.setup();

        bindAbstractVariables();
        buildTestEnvironment();
        bindConcreteVariables();
    }

    private void bindAbstractVariables() {
        entityManager = getHostContext().get(EntityManager.class);
        worldProvider = getHostContext().get(WorldProvider.class);

        BlockManager blockManager = getHostContext().get(BlockManager.class);
        air = blockManager.getBlock("engine:air");
        dirt = blockManager.getBlock("CoreBlocks:Dirt");
    }

    private void buildTestEnvironment() {
        final Vector3i belowTargetPosition = new Vector3i(BUSH_LOCATION).add(Vector3i.down());

        forceAndSetBlock(belowTargetPosition, dirt);
        forceAndSetBlock(BUSH_LOCATION, air);
        plantSeed();
    }

    private void forceAndSetBlock(Vector3i position, Block material) {
        forceAndWaitForGeneration(position);
        worldProvider.setBlock(position, material);
    }

    private void plantSeed() {
        final EntityRef seed = entityManager.create(BUSH_SEED_PREFAB);
        final Vector3f dirtPosition = BUSH_LOCATION.toVector3f().add(Vector3f.down());
        final EntityRef target = entityManager.create(new LocationComponent(dirtPosition));
        seed.send(new ActivateEvent(
                target,          // target
                EntityRef.NULL,  // instigator
                null,            // origin
                null,            // direction
                dirtPosition,    // hit position
                Vector3f.up(),   // hit normal
                0                // activation id
        ));
    }

    private void bindConcreteVariables() {
        BlockEntityRegistry blockEntityRegistry = getHostContext().get(BlockEntityRegistry.class);
        bush = blockEntityRegistry.getExistingBlockEntityAt(BUSH_LOCATION);
        component = bush.getComponent(BushDefinitionComponent.class);
    }

    protected final int getFinalGrowthStageIndex() {
        return component.growthStages.size() - 1;
    }

    protected final void makeBushUnsustainable() {
        component.sustainable = false;
    }

    /**
     * Harvests the test bush.
     * <p>
     * The entity doing the harvesting has no inventory, so produce drops should spawn into the world (and therefore be
     * detectable via {@link #assertActionDropsProduce(Runnable)}).
     */
    protected final void harvestBush() {
        EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        bush.send(new ActivateEvent(
                bush,                    // target
                player,  // instigator
                null,                    // origin
                null,                    // direction
                null,                    // hit position
                null,                    // hit normal
                0                        // activation id
        ));
    }

    protected final void destroyBush() {
        bush.send(new DestroyEvent(
                EntityRef.NULL,                // instigator
                EntityRef.NULL,                // direct cause
                EngineDamageTypes.DIRECT.get() // damage type
        ));
    }

    protected final void waitForGrowth() {
        final int startStage = component.currentStage;
        runWhile(() -> component.currentStage == startStage);
    }

    protected final void waitUntilHarvestable() {
        runUntil(() -> (component.currentStage == getFinalGrowthStageIndex()));
    }

    /**
     * Asserts that the test bush is in the expected growth stage.
     * <p>
     * This checks both that the {@link BushDefinitionComponent#currentStage} field has the correct value, and that the
     * block used is correct.
     */
    protected final void assertBushInStage(int stage) {
        assertEquals(stage, component.currentStage);
        BlockComponent blockComponent = bush.getComponent(BlockComponent.class);
        String block = BushAuthoritySystem.getGrowthStage(component, stage).getKey();
        assertEquals(block, worldProvider.getBlock(blockComponent.getPosition()).toString());
    }

    /**
     * Asserts that both the bush block and the bush entity have been destroyed.
     */
    protected final void assertBushDestroyed() {
        assertFalse(bush.exists());
        assertEquals(air, worldProvider.getBlock(BUSH_LOCATION));
    }

    protected final void assertActionDropsSeeds(Runnable action) {
        assertActionDropsPrefabs(action, component.seed);
    }

    protected final void assertActionDropsProduce(Runnable action) {
        assertActionDropsPrefabs(action, component.produce);
    }

    protected final void assertActionDropsSeedsAndProduce(Runnable action) {
        assertActionDropsPrefabs(action, component.seed, component.produce);
    }

    private void assertActionDropsPrefabs(Runnable action, String... prefabs) {
        final TestEventReceiver<DropItemEvent> dropSpy = new TestEventReceiver<>(getHostContext(), DropItemEvent.class);
        action.run();
        for (String prefab : prefabs) {
            assertListContainsEntityFromPrefab(dropSpy.getEntityRefs(), prefab);
        }
    }

}
