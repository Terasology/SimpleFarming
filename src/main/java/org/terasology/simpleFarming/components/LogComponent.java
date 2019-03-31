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
package org.terasology.simpleFarming.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Vector3i;
import org.terasology.simpleFarming.systems.TreeAuthoritySystem;
import org.terasology.world.block.ForceBlockActive;

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
