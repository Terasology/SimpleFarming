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

import org.junit.Test;
import org.terasology.simpleFarming.testing.SingleBushTestingEnvironment;

public class BushAuthoritySystemTest extends SingleBushTestingEnvironment {

    @Test
    public void bushShouldGrowInOrder() {
        for (int i = 0; i < getFinalGrowthStageIndex(); i++) {
            assertBushInStage(i);
            waitForGrowth();
        }
    }

    @Test
    public void harvestingSustainableBushShouldResetGrowthAndDropProduce() {
        waitUntilHarvestable();
        assertActionDropsProduce(this::harvestBush);
        assertBushInStage(getFinalGrowthStageIndex() - 1);
    }

    @Test
    public void harvestingUnsustainableBushShouldDestroyBushAndDropBothSeedsAndProduce() {
        makeBushUnsustainable();
        waitUntilHarvestable();
        assertActionDropsSeedsAndProduce(this::harvestBush);
        assertBushDestroyed();
    }

    @Test
    public void destroyingMatureBushShouldDropSeeds() {
        waitUntilHarvestable();
        assertActionDropsSeeds(this::destroyBush);
        assertBushDestroyed();
    }
}
