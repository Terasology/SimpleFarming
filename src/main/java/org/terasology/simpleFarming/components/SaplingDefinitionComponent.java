package org.terasology.simpleFarming.components;

import java.util.ArrayList;
import java.util.List;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.block.Block;

public class SaplingDefinitionComponent implements Component {
	
	public Vector3i location;
	
	public Vector3i test;
	
	public Block sapling;
	
	public Block log;
	
	public Prefab leaf;
	
	public List<TreeGrowthStage> growthStages = new ArrayList<>();
	
	public SaplingDefinitionComponent() {
		
	}
	
	public SaplingDefinitionComponent(RootComponent base, Vector3i location) {
		this.location = location;
		sapling = base.sapling;
		log = base.log;
		leaf = base.leaf;
		growthStages = base.growthStages;
	}
}
