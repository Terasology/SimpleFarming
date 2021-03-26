// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.systems;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.Tag;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.logic.players.PlayerCharacterComponent;
import org.terasology.engine.math.Direction;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;

@Tag("MteTest")
public abstract class BaseAuthorityTest {

    public abstract EntityManager getEntityManager();

    public abstract WorldProvider getWorldProvider();

    public abstract ModuleTestingHelper getModuleTestingHelper();

    protected EntityRef playerInstigator() {
        EntityRef player = getEntityManager().create();
        player.addComponent(new PlayerCharacterComponent());
        return player;
    }

    protected void setBlock(Vector3i position, Block block) {
        getModuleTestingHelper().forceAndWaitForGeneration(position);
        getWorldProvider().setBlock(position, block);
    }

    protected void plant(EntityRef seed, Vector3f position) {
        final EntityRef target = getEntityManager().create(new LocationComponent(position));
        seed.send(new ActivateEvent(target, EntityRef.NULL, null, new Vector3f(Direction.UP.asVector3f()), position, new Vector3f(0, 1, 0), 0));
    }
}
