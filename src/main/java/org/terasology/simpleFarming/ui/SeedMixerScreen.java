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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.BaseInteractionScreen;
import org.terasology.rendering.nui.databinding.Binding;
import org.terasology.rendering.nui.databinding.ReadOnlyBinding;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryCell;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;

public class SeedMixerScreen extends BaseInteractionScreen {

    private EntityRef player = EntityRef.NULL;

    private InventoryGrid playerInventory;
    private static final Logger logger = LoggerFactory.getLogger(SeedMixerScreen.class);

    private InventoryCell seed1;
    private InventoryCell seed2;
    private InventoryCell offspring1;
    private InventoryCell offspring2;

    @Override
    protected void initializeWithInteractionTarget(EntityRef interactionTarget) {
        logger.info(interactionTarget.toFullDescription());
        logger.info(String.valueOf(interactionTarget.hasComponent(InventoryComponent.class)));
        seed1.bindTargetInventory(new Binding<EntityRef>() {
            @Override
            public EntityRef get() {
                return interactionTarget;
            }

            @Override
            public void set(EntityRef value) {
                seed1.setTargetInventory(value);
            }
        });
        seed2.bindTargetInventory(new Binding<EntityRef>() {
            @Override
            public EntityRef get() {
                return interactionTarget;
            }

            @Override
            public void set(EntityRef value) {
                seed2.setTargetInventory(value);
            }
        });
        offspring1.bindTargetInventory(new Binding<EntityRef>() {
            @Override
            public EntityRef get() {
                return interactionTarget;
            }

            @Override
            public void set(EntityRef value) {
                offspring1.setTargetInventory(value);
            }
        });
        offspring2.bindTargetInventory(new Binding<EntityRef>() {
            @Override
            public EntityRef get() {
                return interactionTarget;
            }

            @Override
            public void set(EntityRef value) {
                offspring2.setTargetInventory(value);
            }
        });
    }

    private void reInit() {
        player = CoreRegistry.get(LocalPlayer.class).getCharacterEntity();
        playerInventory = find("inventory", InventoryGrid.class);
        playerInventory.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
            @Override
            public EntityRef get() {
                return player;
            }
        });
        playerInventory.setMaxCellCount(40);
    }

    @Override
    public void initialise() {
        player = EntityRef.NULL;

        seed1 = find("S1", InventoryCell.class);
        seed2 = find("S2", InventoryCell.class);
        offspring1 = find("O1", InventoryCell.class);
        offspring2 = find("O2", InventoryCell.class);
    }

    @Override
    public void onOpened() {
        EntityRef characterEntity = CoreRegistry.get(LocalPlayer.class).getCharacterEntity();
        CharacterComponent characterComponent = characterEntity.getComponent(CharacterComponent.class);

        // In case the player has been created yet, exit out early to prevent an error.
        if (characterComponent == null) {
            logger.info("character component null");
            return;
        }

        // If the reference to the player entity hasn't been set yet, or it refers to a NULL entity, call the reInit()
        // method to set it. The getId() check is necessary for certain network entities whose ID is 0, but are
        // erroneously marked as existent.
        if (!player.exists() || (player.exists() && (player == EntityRef.NULL || player.getId() == 0 || player == null))) {
            reInit();
        }

        // As long as there's an interaction target, open this window.
        if (getInteractionTarget() != EntityRef.NULL) {
            initializeWithInteractionTarget(getInteractionTarget());
            super.onOpened();
        }
    }
}
