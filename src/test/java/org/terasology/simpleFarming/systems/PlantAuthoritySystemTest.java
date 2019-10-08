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

import org.junit.Before;
import org.junit.Test;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.components.BushGrowthStage;
import org.terasology.simpleFarming.components.SeedDefinitionComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PlantAuthoritySystemTest extends ModuleTestingEnvironment {

    /** The block our test bush should use. */
    private static final String BUSH_BLOCK = "SimpleFarming:BaseBush";

    private EntityManager entityManager;
    private WorldProvider worldProvider;

    private Block air;
    private Block dirt;
    private Block bush;

    @Before
    public void initialize() {
        entityManager = getHostContext().get(EntityManager.class);
        worldProvider = getHostContext().get(WorldProvider.class);

        BlockManager blockManager = getHostContext().get(BlockManager.class);
        air = blockManager.getBlock("engine:air");
        dirt = blockManager.getBlock("CoreBlocks:Dirt");
        bush = blockManager.getBlock(BUSH_BLOCK);
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.singleton("SimpleFarming");
    }

    @Test
    public void seedShouldGrowWherePlanted() {
        plantSeedTest(dirt, air, true);
    }

    @Test
    public void seedShouldNotGrowOnAir() {
        plantSeedTest(air, air, false);
    }

    @Test
    public void seedShouldNotReplaceBlock() {
        plantSeedTest(dirt, dirt, false);
    }

    /**
     * Generic test trying to plant a seed and observing the result.
     * <p>
     * Sets the target block and the block above the target to the given materials and attempts to
     * plant a seed on the target block.  Then makes assertions according to the value of
     * {@code shouldSucceed}: if {@code shouldSucceed} is true, the block above the target should
     * now be the block specified by {@code BUSH_BLOCK}; if not, it should remain
     * {@code materialAbove}.  In either case, the target block should be unchanged.
     *
     * @param material the block the seed should be planted on
     * @param materialAbove the block above the target block
     * @param shouldSucceed whether the planting is expected to succeed
     */
    private void plantSeedTest(Block material, Block materialAbove, boolean shouldSucceed) {
        final Vector3i targetPosition = Vector3i.zero();
        final Vector3i aboveTargetPosition = new Vector3i(targetPosition).add(Vector3i.up());

        setBlock(targetPosition, material);
        setBlock(aboveTargetPosition, materialAbove);
        plantSeed(makeTestSeed(), targetPosition);

        final Block expected = shouldSucceed ? bush : materialAbove;
        assertEquals(expected, worldProvider.getBlock(aboveTargetPosition));
        assertEquals(material, worldProvider.getBlock(targetPosition));
    }

    /**
     * Fills the given region with the specified block.
     * <p>
     * Also ensures that all blocks in and adjacent to the region are loaded.
     */
    private void fillRegion(Region3i region, Block material) {
        Region3i loadRegion = region.expand(1);
        for (Vector3i pos : loadRegion) {
            forceAndWaitForGeneration(pos);
        }

        for (Vector3i pos : region) {
            worldProvider.setBlock(pos, material);
        }
    }

    /**
     * Sets the block at the given position.
     * <p>
     * Also ensures that all blocks adjacent to the position are loaded.
     */
    private void setBlock(Vector3i position, Block material) {
        fillRegion(Region3i.createFromCenterExtents(position, 0), material);
    }

    /**
     * Creates a seed entity, using parameters appropriate to the test.
     * <p>
     * To avoid coupling the test to any particular seed prefab, generates a seed definition from
     * scratch, with a null prefab and a basic, single-stage bush definition on the same entity.
     */
    private EntityRef makeTestSeed() {
        final BushDefinitionComponent bushDefinition = new BushDefinitionComponent();
        bushDefinition.growthStages = Collections.singletonMap(BUSH_BLOCK, new BushGrowthStage());

        return entityManager.create(
                new SeedDefinitionComponent(),
                bushDefinition
        );
    }

    /** Attempts to plant the given seed at the given position. */
    private void plantSeed(EntityRef seed, Vector3i position) {
        final EntityRef target = entityManager.create(new LocationComponent(position.toVector3f()));
        final ActivateEvent event = new ActivateEvent(
                target,                 // target
                EntityRef.NULL,         // instigator
                null,                   // origin
                null,                   // direction
                position.toVector3f(),  // hit position
                Vector3f.up(),          // hit normal
                0                       // activation id
        );
        seed.send(event);
    }
}
