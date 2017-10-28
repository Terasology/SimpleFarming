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

/**
 * Component used to store information about the current state of a particular vine.
 * <p>
 * Each {@link VineDefinitionComponent#stem} block that is part of the vine has a block entity
 * associated with a different instance of this component; these instances are linked together by
 * the {@link #child} and {@link #parent} fields as a doubly-linked list.
 *
 * @see org.terasology.simpleFarming.systems.VineAuthoritySystem
 */
public class VineNodeComponent implements Component {

    /** The position of this stem block. */
    public Vector3i position;

    /**
     * Any bud attached to this stem block.  Null if there is no bud yet.
     * <p>
     * The bud should have a reciprocal link to this entity in its
     * {@link BushDefinitionComponent#parent} field.
     * <p>
     * Note that each stem block can produce at most one bud at a time.
     */
    public EntityRef bud;

    /** The adjacent stem block that is further from the root.  Null if this is the vine tip. */
    public EntityRef child;

    /** The adjacent stem block that is closer to the root.  Null if this is the root. */
    public EntityRef parent;

    /** The distance (through the vine) from this block to the root.  Zero if this is the root. */
    public int height;

    /** Default constructor required for persistence. */
    public VineNodeComponent() {
    }

    /** Construct a component at the given position. */
    public VineNodeComponent(Vector3i position) {
        this.position = position;
    }

    /**
     * Construct a component at the given position, with the given parent.
     * <p>
     * Does not establish the reciprocal link from parent to child.
     */
    public VineNodeComponent(EntityRef parent, Vector3i position) {
        this.parent = parent;
        this.position = position;
    }
}
