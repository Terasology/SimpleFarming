// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.simpleFarming.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.genome.GenomeRegistry;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.genome.genomeMap.GenomeMap;
import org.terasology.simpleFarming.events.ModifyFilling;
import org.terasology.simpleFarming.events.ModifyTint;

public final class GenomeUtil {

    private GenomeUtil() {
    }

    public static void updateFilling(GenomeRegistry genomeRegistry, EntityRef entity) {
        GenomeComponent genome = entity.getComponent(GenomeComponent.class);
        GenomeMap genomeMap = genomeRegistry.getGenomeDefinition(genome.genomeId).getGenomeMap();
        float fillingModifier = genomeMap.getProperty("filling", genome.genes, Float.class);

        float newFilling = entity.send(new ModifyFilling(fillingModifier)).filling.getValue();

        entity.send(new ModifyTint(newFilling));
    }
}
