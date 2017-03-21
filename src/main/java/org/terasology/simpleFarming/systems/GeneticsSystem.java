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
package org.terasology.simpleFarming.systems;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.genome.GenomeDefinition;
import org.terasology.genome.GenomeRegistry;
import org.terasology.genome.breed.BreedingAlgorithm;
import org.terasology.genome.breed.MonoploidBreedingAlgorithm;
import org.terasology.genome.breed.mutator.GeneMutator;
import org.terasology.genome.breed.mutator.VocabularyGeneMutator;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.genome.events.OnBreed;
import org.terasology.genome.system.SimpleGenomeManager;
import org.terasology.genome.genomeMap.SeedBasedGenomeMap;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;

import javax.annotation.Nullable;

@RegisterSystem
public class GeneticsSystem extends BaseComponentSystem {
    @In
    private WorldProvider worldProvider;

    @In
    private GenomeRegistry genomeRegistry;

    private static final Logger logger = LoggerFactory.getLogger(GeneticsSystem.class);

    @Override
    public void preBegin() {
        int genomeLength = 1;

        //A=5, F=10, K=15
        GeneMutator geneMutator = new VocabularyGeneMutator("ABCDEFGHIJK");
        BreedingAlgorithm breedingAlgorithm = new MonoploidBreedingAlgorithm(0, 0.4f, geneMutator);

        SeedBasedGenomeMap genomeMap = new SeedBasedGenomeMap(worldProvider.getSeed().hashCode());
        genomeMap.addSeedBasedProperty("filling", 1, genomeLength, 1, Integer.class,
                new Function<String, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable String input) {
                        return (input.charAt(0) - 'A' + 5);
                    }
                });

        GenomeDefinition genomeDefinition = new GenomeDefinition(breedingAlgorithm, genomeMap);
        genomeRegistry.registerType("blueberry", genomeDefinition);
    }
}
