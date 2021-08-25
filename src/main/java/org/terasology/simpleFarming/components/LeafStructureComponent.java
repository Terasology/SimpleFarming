// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.joml.Vector3i;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the structure of leaves in a tree.
 *
 * @see TreeGrowthStage
 */
public class LeafStructureComponent implements Component<LeafStructureComponent> {
    /**
     * Locations of each leaf in this structure, relative to the top log block.
     */
    public Set<Vector3i> leaves = new HashSet<>();

    @Override
    public void copyFrom(LeafStructureComponent other) {
        this.leaves = other.leaves.stream()
                .map(Vector3i::new)
                .collect(Collectors.toSet());
    }
}
