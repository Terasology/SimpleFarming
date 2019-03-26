package org.terasology.simpleFarming.components;

import org.terasology.entitySystem.Component;
import org.terasology.math.geom.Vector3i;

import java.util.HashSet;
import java.util.Set;

/**
 * Defines the structure of leaves in a tree.
 *
 * @see TreeGrowthStage
 */
public class LeafStructureComponent implements Component {
    /**
     * Locations of each leaf in this structure, relative to the top log block.
     */
    public Set<Vector3i> leaves = new HashSet<>();
}
