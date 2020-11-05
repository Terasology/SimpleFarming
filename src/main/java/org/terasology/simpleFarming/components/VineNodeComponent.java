// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.joml.Vector3i;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.world.block.ForceBlockActive;

/**
 * Component used to store information about the current state of a particular vine.
 * <p>
 * Each {@link VineDefinitionComponent#stem} block that is part of the vine has a block entity
 * associated with a different instance of this component; these instances are linked together by
 * the {@link #child} and {@link #parent} fields as a doubly-linked list.
 *
 * @see org.terasology.simpleFarming.systems.VineAuthoritySystem
 */
@ForceBlockActive
public class VineNodeComponent implements Component {

    /**
     * The position of this stem block.
     */
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

    /**
     * The adjacent stem block that is further from the root.  Null if this is the vine tip.
     */
    public EntityRef child;

    /**
     * The adjacent stem block that is closer to the root.  Null if this is the root.
     */
    public EntityRef parent;

    /**
     * The distance (through the vine) from this block to the root.  Zero if this is the root.
     */
    public int length;

    /**
     * Default constructor required for persistence.
     */
    public VineNodeComponent() {
    }

    /**
     * Construct a component at the given position.
     */
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
