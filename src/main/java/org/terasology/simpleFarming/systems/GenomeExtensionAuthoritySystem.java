// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.systems;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.RetainComponentsComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.world.WorldProvider;
import org.terasology.genome.GenomeDefinition;
import org.terasology.genome.GenomeRegistry;
import org.terasology.genome.breed.BreedingAlgorithm;
import org.terasology.genome.breed.ContinuousBreedingAlgorithm;
import org.terasology.genome.breed.mutator.GeneMutator;
import org.terasology.genome.breed.mutator.VocabularyGeneMutator;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.genome.genomeMap.SeedBasedGenomeMap;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.events.AddGenomeRetention;
import org.terasology.simpleFarming.events.BeforePlanted;
import org.terasology.simpleFarming.events.ProduceCreated;
import org.terasology.simpleFarming.events.TransferGenomeEvent;

import javax.annotation.Nullable;

/**
 * Extension system managing genetics of all plants.
 *
 * This component system is only loaded if the "Genome" module is active.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class GenomeExtensionAuthoritySystem extends BaseComponentSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenomeExtensionAuthoritySystem.class);

    @In
    private GenomeRegistry genomeRegistry;
    @In
    private WorldProvider worldProvider;

    /**
     * Called immediately after a bush has been harvested.
     * <p>
     * Checks the genome component of the bush and assigns it to the seed if it had genes already. If the bush did not have a
     * GenomeComponent, it is assigned as this is the first harvest A new Genome Definition is created for the family
     *
     * @param event the Produce created event
     * @param creator the creator(bush) of the produce
     */
    @ReceiveEvent
    public void onProduceCreated(ProduceCreated event, EntityRef creator) {
        EntityRef producer = event.getCreator();
        GenomeComponent genomeComponent = new GenomeComponent();
        EntityRef produce = event.getProduce();
        if (producer.hasComponent(GenomeComponent.class)) {
            genomeComponent.genomeId = producer.getComponent(GenomeComponent.class).genomeId;
            genomeComponent.genes = producer.getComponent(GenomeComponent.class).genes;
        } else {
            FastRandom rand = new FastRandom();
            genomeComponent.genomeId = producer.getParentPrefab().getName();
            if (genomeRegistry.getGenomeDefinition(genomeComponent.genomeId) == null) {
                LOGGER.info("Defining new genome map for " + genomeComponent.genomeId);
                addPropertyMap(producer, genomeComponent.genomeId);
            }
            //TODO : needs to be random based on vocabulary
            genomeComponent.genes =
                    "" + "ABCDEFGHIJK".charAt(rand.nextInt(9)) + "" + "ABCDEFGHIJK".charAt(rand.nextInt(9)) + "" +
                            "ABCDEFGHIJK".charAt(rand.nextInt(9));
            if (producer != null) {
                producer.addOrSaveComponent(genomeComponent);
            }
        }
        produce.addOrSaveComponent(genomeComponent);

        GenomeUtil.updateFilling(genomeRegistry, produce);
    }

    /**
     * Called immediately after a bush has been harvested.
     * <p>
     * Checks the genome component of the seed and transfers it to the plant that was planted if the seed does not contain a genome
     * component, the component is assigned on first harvest
     *
     * @param event the Before Planted event
     * @param plant the plant that is being planted
     */
    @ReceiveEvent
    public void onBeforePlantedEvent(BeforePlanted event, EntityRef plant) {
        EntityRef seed = event.getSeed();
        if (seed.hasComponent(GenomeComponent.class)) {
            plant.addOrSaveComponent(seed.getComponent(GenomeComponent.class));
        }
    }

    /**
     * Transfers the GenomeComponent across different stages of bush growth
     *
     * @param event the Transfer Genome Event
     * @param bush the bush that is growing
     * @param bushComponent bushComponent to check if it is a bush
     * @param genomeComponent genomeComponent to check if the bush has a genomeComponent to pass on
     */
    @ReceiveEvent
    public void onTransferGenomeEvent(TransferGenomeEvent event, EntityRef bush,
                                      BushDefinitionComponent bushComponent, GenomeComponent genomeComponent) {
        event.getTransferEntity().addOrSaveComponent(genomeComponent);
    }

    /**
     * Adds the GenomeComponent to the RetainComponentsComponent of an entity Event handler added to maintain Genome optional dependency
     *
     * @param event the AddGenomeRetention event
     * @param entity the entity whose RetainComponentsComponent is to be modified
     */
    @ReceiveEvent
    public void addGenomeRetentionEvent(AddGenomeRetention event, EntityRef entity) {
        RetainComponentsComponent retainComponentsComponent = new RetainComponentsComponent();
        retainComponentsComponent.components.add(GenomeComponent.class);
        entity.addOrSaveComponent(retainComponentsComponent);
    }

    private void addPropertyMap(EntityRef entity, String genomeId) {
        SeedBasedGenomeMap genomeMap = new SeedBasedGenomeMap(worldProvider.getSeed().hashCode());
        String geneVocabulary = "ABCDEFGHIJK";
        GeneMutator geneMutator = new VocabularyGeneMutator(geneVocabulary);
        BreedingAlgorithm continuousBreedingAlgorithm = new ContinuousBreedingAlgorithm(0.3f, geneMutator);
        genomeMap.addSeedBasedProperty("filling", 0, 2, 3, Float.class, continuousBreedingAlgorithm,
                new Function<String, Float>() {
                    @Nullable
                    @Override
                    public Float apply(@Nullable String input) {
                        return (input.charAt(0) - 'A' + 5f) / 5f;
                    }
                });
        GenomeDefinition genomeDefinition = new GenomeDefinition(continuousBreedingAlgorithm, genomeMap);
        genomeRegistry.registerType(genomeId, genomeDefinition);
    }
}
