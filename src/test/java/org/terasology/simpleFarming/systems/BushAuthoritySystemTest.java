// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.systems;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.health.DestroyEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.moduletestingenvironment.IsolatedMTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.TestEventReceiver;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.extension.UseWorldGenerator;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@ExtendWith(IsolatedMTEExtension.class)
@UseWorldGenerator("ModuleTestingEnvironment:empty")
@Dependencies({"SimpleFarming", "CoreAssets", "ModuleTestingEnvironment"})
public class BushAuthoritySystemTest extends BaseAuthorityTest {

    private Block air;
    private Block dirt;

    @In
    EntityManager entityManager;
    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    ModuleTestingHelper helper;
    @In
    BlockEntityRegistry blockEntityRegistry;

    @BeforeEach
    public void initialize() {
        air = blockManager.getBlock("engine:air");
        dirt = blockManager.getBlock("CoreAssets:Dirt");

        setBlock(new Vector3i(0, -1, 0), dirt);
        setBlock(new Vector3i(), air);

        final EntityRef seed = entityManager.create("SimpleFarming:TestSeed");
        this.plant(seed, new Vector3f());

    }

    @Test
    public void bushShouldGrowInOrder() {
        EntityRef entity = blockEntityRegistry.getExistingBlockEntityAt(new Vector3i());
        BushDefinitionComponent component = entity.getComponent(BushDefinitionComponent.class);
        for (int stage = 0; stage < component.growthStages.size(); stage++) {
            // verify the the bush is on the current stage
            assertEquals(stage, component.currentStage);
            BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
            String block = BushAuthoritySystem.getGrowthStage(component, stage).getKey();
            assertEquals(block, worldProvider.getBlock(blockComponent.getPosition()).toString());

            final int startStage = component.currentStage;
            helper.runWhile(() -> component.currentStage == startStage);
        }
    }

    @Test
    public void harvestingSustainableBushShouldResetGrowthAndDropProduce() {
        EntityRef entity = blockEntityRegistry.getExistingBlockEntityAt(new Vector3i());
        Assertions.assertNotNull(entity);
        BushDefinitionComponent component = entity.getComponent(BushDefinitionComponent.class);

        // wait until the bush gets to the final growth state
        helper.runUntil(() -> (component.currentStage == (component.growthStages.size() - 1)));

        // harvest bush
        final TestEventReceiver<DropItemEvent> dropSpy = new TestEventReceiver<>(helper.getHostContext(), DropItemEvent.class);
        entity.send(new ActivateEvent(entity, playerInstigator(), null, null, null, null, 0));

        // check if bush drops produce
        Assertions.assertTrue(dropSpy.getEntityRefs().stream().anyMatch(k -> k.getParentPrefab().getName().equals(component.produce)));

        // check if bush is at max growth state
        assertEquals(component.growthStages.size() - 2, component.currentStage);
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        String block = BushAuthoritySystem.getGrowthStage(component, component.growthStages.size() - 2).getKey();
        assertEquals(block, worldProvider.getBlock(blockComponent.getPosition()).toString());
    }

    @Test
    public void harvestingUnsustainableBushShouldDestroyBushAndDropBothSeedsAndProduce() {
        EntityRef entity = blockEntityRegistry.getExistingBlockEntityAt(new Vector3i());
        Assertions.assertNotNull(entity);
        BushDefinitionComponent component = entity.getComponent(BushDefinitionComponent.class);
        component.sustainable = false; // set bush to unsustainable

        // wait until the bush gets to the final growth state
        helper.runUntil(() -> (component.currentStage == (component.growthStages.size() - 1)));

        // harvest bush
        final TestEventReceiver<DropItemEvent> dropSpy = new TestEventReceiver<>(helper.getHostContext(), DropItemEvent.class);
        entity.send(new ActivateEvent(entity, playerInstigator(), null, null, null, null, 0));

        // check if a seed and produce is dropped
        Assertions.assertTrue(dropSpy.getEntityRefs().stream().anyMatch(k -> k.getParentPrefab().getName().equals(component.produce)));
        Assertions.assertTrue(dropSpy.getEntityRefs().stream().anyMatch(k -> k.getParentPrefab().getName().equals(component.seed)));

        // bush removed and replaced with air
        assertFalse(entity.exists());
        assertEquals(air, worldProvider.getBlock(new Vector3f()));
    }

    @Test
    public void destroyingMatureBushShouldDropSeeds() {
        EntityRef entity = blockEntityRegistry.getExistingBlockEntityAt(new Vector3i());
        Assertions.assertNotNull(entity);
        BushDefinitionComponent component = entity.getComponent(BushDefinitionComponent.class);

        // wait until the bush gets to the final growth state
        helper.runUntil(() -> (component.currentStage == (component.growthStages.size() - 1)));

        final TestEventReceiver<DropItemEvent> dropSpy = new TestEventReceiver<>(helper.getHostContext(), DropItemEvent.class);
        entity.send(new DestroyEvent(EntityRef.NULL, EntityRef.NULL, EngineDamageTypes.DIRECT.get()));

        // check if seed is dropped
        Assertions.assertTrue(dropSpy.getEntityRefs().stream().anyMatch(k -> k.getParentPrefab().getName().equals(component.seed)));

        // bush removed and replaced with air
        assertFalse(entity.exists());
        assertEquals(air, worldProvider.getBlock(new Vector3f()));
    }


    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public WorldProvider getWorldProvider() {
        return worldProvider;
    }

    @Override
    public ModuleTestingHelper getModuleTestingHelper() {
        return helper;
    }
}
