// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.terasology.entitySystem.Component;

/**
 * Indicates the item is a seed and links to the prefab to be used to create the plant.
 * <p>
 * An item with this component can be used (right-clicked) on the ground to plant it.  This will
 * consume the seed and create the kind of plant (bush or vine) associated with this seed type.
 *
 * @see org.terasology.simpleFarming.systems.PlantAuthoritySystem
 */
public class SeedDefinitionComponent implements Component {
    /**
     * The prefab name for the plant that grows from this seed.
     * <p>
     * The prefab with this name is expected to have a component that responds to the
     * {@link org.terasology.simpleFarming.events.OnSeedPlanted} event, e.g.
     * {@link BushDefinitionComponent} or {@link VineDefinitionComponent}.  Alternatively, this
     * field may be null; this indicates that the necessary plant definition component is expected
     * to be found on the same entity.
     */
    public String prefab;
}
