// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.systems;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.crafting.events.OnRecipeCrafted;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.genome.GenomeDefinition;
import org.terasology.genome.GenomeRegistry;
import org.terasology.genome.breed.BreedingAlgorithm;
import org.terasology.genome.breed.ContinuousBreedingAlgorithm;
import org.terasology.genome.breed.mutator.GeneMutator;
import org.terasology.genome.breed.mutator.VocabularyGeneMutator;
import org.terasology.genome.component.GenomeComponent;
import org.terasology.genome.genomeMap.GenomeMap;
import org.terasology.genome.genomeMap.SeedBasedGenomeMap;
import org.terasology.genome.system.SimpleGenomeManager;
import org.terasology.logic.common.RetainComponentsComponent;
import org.terasology.registry.In;
import org.terasology.simpleFarming.components.BushDefinitionComponent;
import org.terasology.simpleFarming.events.AddGenomeRetention;
import org.terasology.simpleFarming.events.BeforePlanted;
import org.terasology.simpleFarming.events.ModifyFilling;
import org.terasology.simpleFarming.events.ModifyTint;
import org.terasology.simpleFarming.events.ProduceCreated;
import org.terasology.simpleFarming.events.TransferGenomeEvent;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.WorldProvider;

import javax.annotation.Nullable;

/**
 * System managing genetics of all plants
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class GenomeAuthoritySystem extends BaseComponentSystem {
    @In
    private GenomeRegistry genomeRegistry;
    @In
    private WorldProvider worldProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(GenomeAuthoritySystem.class);

    /**
     * Called immediately after a bush has been harvested.
     * <p>
     * Checks the genome component of the bush and assigns it to the seed if it had genes already.
     * If the bush did not have a GenomeComponent, it is assigned as this is the first harvest
     * A new Genome Definition is created for the family
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

        GenomeMap genomeMap =
                genomeRegistry.getGenomeDefinition(produce.getComponent(GenomeComponent.class).genomeId).getGenomeMap();
        float fillingModifier = genomeMap.getProperty("filling", produce.getComponent(GenomeComponent.class).genes,
                Float.class);
        float newFilling = produce.send(new ModifyFilling(fillingModifier)).filling.getValue();
        produce.send(new ModifyTint(newFilling));
    }

    /**
     * Called immediately after a bush has been harvested.
     * <p>
     * Checks the genome component of the seed and transfers it to the plant that was planted
     * if the seed does not contain a genome component, the component is assigned on first harvest
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
     * Adds the GenomeComponent to the RetainComponentsComponent of an entity Event handler added to maintain Genome
     * optional dependency
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

    /**
     * Adds genes to the crafted entity if breeding is possible.
     * @param event the OnRecipeCrafted event
     * @param entity the crafted entity which is to be modified
     */
    @ReceiveEvent
    public void onRecipeCraftedEvent(OnRecipeCrafted event, EntityRef entity) {
        EntityRef ingredients[] = event.getIngredients();
        if (ingredients.length != 2) {
            return;
        }

        if (!(ingredients[0].hasComponent(GenomeComponent.class) || ingredients[1].hasComponent(GenomeComponent.class))) {
            return;
        }

        SimpleGenomeManager genomeManager = new SimpleGenomeManager();
        boolean result = genomeManager.applyBreeding(ingredients[0], ingredients[1], entity);
        if (entity.hasComponent(GenomeComponent.class)) {
            GenomeMap genomeMap =
                    genomeRegistry.getGenomeDefinition(entity.getComponent(GenomeComponent.class).genomeId).getGenomeMap();
            float fillingModifier = genomeMap.getProperty("filling", entity.getComponent(GenomeComponent.class).genes,
                    Float.class);
            float newFilling = entity.send(new ModifyFilling(fillingModifier)).filling.getValue();
            entity.send(new ModifyTint(newFilling));
        }
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
                        return (input.charAt(0) - 'A' + 5f)/5f;
                    }
                });
        GenomeDefinition genomeDefinition = new GenomeDefinition(continuousBreedingAlgorithm, genomeMap);
        genomeRegistry.registerType(genomeId, genomeDefinition);
    }
}
