package org.terasology.simpleFarming.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.world.block.Block;
import org.terasology.world.block.ForceBlockActive;

@ForceBlockActive
public class RootComponent implements Component {
	
	public Block sapling;
	
	public Block log;
	
	public Prefab leaf;
	
	public List<TreeGrowthStage> growthStages = new ArrayList<>();
	
	public int growthStage = 0;
	
	public Set<EntityRef> leaves = new HashSet<>();
	
	public boolean alive = true;
	
	public RootComponent() {
		
	}
	
	public RootComponent(SaplingDefinitionComponent base) {
		sapling = base.sapling;
		log = base.log;
		leaf = base.leaf;
		growthStages = base.growthStages;
	}
}
