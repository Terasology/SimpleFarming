// Copyright 2020 The Terasology Foundation
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
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.extension.UseWorldGenerator;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.BushGrowthStage;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

import java.util.Collections;


@ExtendWith(MTEExtension.class)
@UseWorldGenerator("ModuleTestingEnvironment:empty")
@Dependencies({"SimpleFarming", "CoreAssets", "ModuleTestingEnvironment"})
public class PlantAuthoritySystemTest extends BaseAuthorityTest {

    /**
     * The block our test bush should use.
     */
    private static final String BUSH_BLOCK = "SimpleFarming:BaseBush";

    @In
    EntityManager entityManager;
    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    ModuleTestingHelper helper;


    private Block air;
    private Block dirt;
    private Block bush;

    @BeforeEach
    public void initialize() {
        air = blockManager.getBlock("engine:air");
        dirt = blockManager.getBlock("CoreAssets:Dirt");
        bush = blockManager.getBlock(BUSH_BLOCK);
    }

    @Test
    public void seedShouldGrowWherePlanted() {
        setBlock(new Vector3i(), dirt);
        setBlock(new Vector3i(0, 1, 0), air);

        EntityRef seed = testSeed();
        plant(seed, new Vector3f());

        Assertions.assertEquals(bush, worldProvider.getBlock(new Vector3i(0, 1, 0)));
        Assertions.assertEquals(dirt, worldProvider.getBlock(new Vector3i(0, 0, 0)));
    }

    @Test
    public void tryPlantSeedOnAir() {
        setBlock(new Vector3i(), air);
        setBlock(new Vector3i(0, 1, 0), air);

        EntityRef seed = testSeed();
        plant(seed, new Vector3f());

        Assertions.assertEquals(air, worldProvider.getBlock(new Vector3i(0, 1, 0)));
        Assertions.assertEquals(air, worldProvider.getBlock(new Vector3i(0, 0, 0)));
    }

    @Test
    public void seedShouldNotReplaceBlock() {
        setBlock(new Vector3i(), dirt);
        setBlock(new Vector3i(0, 1, 0), dirt);

        EntityRef seed = testSeed();
        plant(seed, new Vector3f());

        Assertions.assertEquals(dirt, worldProvider.getBlock(new Vector3i(0, 1, 0)));
        Assertions.assertEquals(dirt, worldProvider.getBlock(new Vector3i(0, 0, 0)));
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

    private EntityRef testSeed() {
        final BushDefinitionComponent bushDefinition = new BushDefinitionComponent();
        bushDefinition.growthStages = Collections.singletonMap(BUSH_BLOCK, new BushGrowthStage());

        return entityManager.create(
            new SeedDefinitionComponent(),
            bushDefinition
        );
    }
}
