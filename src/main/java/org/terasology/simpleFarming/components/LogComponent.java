package org.terasology.simpleFarming.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.block.ForceBlockActive;

@ForceBlockActive
public class LogComponent implements Component {
	
	public Vector3i location;
	
	public EntityRef root;
}
