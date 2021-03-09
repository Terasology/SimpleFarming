// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.joml.Vector3i;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.simpleFarming.systems.TreeAuthoritySystem;

/**
 * Represents any log in a tree, including the root.
 * Created automatically by {@link TreeAuthoritySystem}.
 *
 * @see RootComponent
 */
@ForceBlockActive
public class LogComponent implements Component {

    /**
     * The location of this log in the world.
     */
    public Vector3i location;

    /**
     * The root associated with this log's tree. This reference helps
     * to identify which tree this log is part of and send important
     * events to the root so that the whole tree can react.
     */
    public EntityRef root;
}
