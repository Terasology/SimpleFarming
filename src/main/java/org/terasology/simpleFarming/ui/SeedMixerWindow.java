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
package org.terasology.simpleFarming.ui;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.rendering.nui.BaseInteractionScreen;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;

/**
 * This class represents the seed mixer window.
 */
public class SeedMixerWindow extends BaseInteractionScreen {

    private InventoryGrid playerInventory;

    private InventoryGrid seed1;
    private InventoryGrid seed2;
    private InventoryGrid offspring1;
    private InventoryGrid offspring2;

    private EntityRef player = EntityRef.NULL;

    @Override
    public void initialise() {
        player = EntityRef.NULL;

        seed1 = find("S1", InventoryGrid.class);
        seed2 = find("S1", InventoryGrid.class);
        offspring1 = find("O1", InventoryGrid.class);
        offspring2 = find("O2", InventoryGrid.class);
    }

    @Override
    protected void initializeWithInteractionTarget(EntityRef interactionTarget) {
    }

}
