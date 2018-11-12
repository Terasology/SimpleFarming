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
package org.terasology.simpleFarming.components;

import java.util.HashSet;
import java.util.Set;

import org.terasology.math.geom.Vector3i;
import org.terasology.reflection.MappedContainer;

/**
 * Represents a stage of growth for a tree.
 * <p>
 * Contains the positions of leaves relative to the root block and the number of logs this growth stage should contain.
 * 
 * @see SaplingDefinitionComponent
 */
@MappedContainer
public class TreeGrowthStage {
    /** Number of logs this stage should have. */
	public int height;
	
	/** Locations of each leaf in this stage, relative to the root (the lowest log block). */
	public Set<Vector3i> leaves = new HashSet<>();
    
    /**
     * Default leaf configuration to use, 0 if custom defined.
     * There are currently 3 default leaf configurations.
     */
    public int defLeaves;

    /** Minimum time before this growth stage, in milliseconds. */
    public int minTime;

    /** Maximum time before this growth stage, in milliseconds. */
    public int maxTime;

    /** Default constructor required for persistence. */
    public TreeGrowthStage() {
    }

    /** Construct a copy of the given {@code GrowthStage}. */
    public TreeGrowthStage(TreeGrowthStage clone) {
    	this.height = clone.height;
    	this.leaves = clone.leaves;
        this.maxTime = clone.maxTime;
        this.minTime = clone.minTime;
    }
    
    /**
     * Gets the leaf locations for this growth stage.
     * If using a default configuration, the leaves will automatically be adjusted to the {@link #height}
     * 
     * @return A Set of all of the locations where a leaf should be grown.
     */
    public Set<Vector3i> getLeaves() {
    	if(defLeaves == 0) {
    		return leaves;
    	}
    	
    	Vector3i location = new Vector3i(0, height-1, 0);
    	Set<Vector3i> defLeafSet = new HashSet<>();
    	switch(defLeaves) {
    		case 3:
    			defLeafSet.add(new Vector3i(0, 2, 0).add(location));
    			defLeafSet.add(new Vector3i(1, 2, 0).add(location));
    			defLeafSet.add(new Vector3i(-1, 2, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 2, 1).add(location));
    			defLeafSet.add(new Vector3i(0, 2, -1).add(location));
    			defLeafSet.add(new Vector3i(1, 2, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, 2, 1).add(location));
    			defLeafSet.add(new Vector3i(1, 2, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, 2, -1).add(location));

    			defLeafSet.add(new Vector3i(2, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(2, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(2, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(2, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(2, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(2, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(2, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(2, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(2, -1, -1).add(location));

    			defLeafSet.add(new Vector3i(-2, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(-2, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(-2, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(-2, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(-2, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(-2, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(-2, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(-2, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(-2, -1, -1).add(location));

    			defLeafSet.add(new Vector3i(1, 1, 2).add(location));
    			defLeafSet.add(new Vector3i(0, 1, 2).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, 2).add(location));
    			defLeafSet.add(new Vector3i(1, 0, 2).add(location));
    			defLeafSet.add(new Vector3i(0, 0, 2).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, 2).add(location));
    			defLeafSet.add(new Vector3i(1, -1, 2).add(location));
    			defLeafSet.add(new Vector3i(0, -1, 2).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, 2).add(location));

    			defLeafSet.add(new Vector3i(1, 1, -2).add(location));
    			defLeafSet.add(new Vector3i(0, 1, -2).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, -2).add(location));
    			defLeafSet.add(new Vector3i(1, 0, -2).add(location));
    			defLeafSet.add(new Vector3i(0, 0, -2).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, -2).add(location));
    			defLeafSet.add(new Vector3i(1, -1, -2).add(location));
    			defLeafSet.add(new Vector3i(0, -1, -2).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, -2).add(location));
    		case 2:
    			defLeafSet.add(new Vector3i(-1, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(1, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, -1).add(location));
    			defLeafSet.add(new Vector3i(1, 1, 1).add(location));
    			
    			defLeafSet.add(new Vector3i(1, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 1, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, 1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 1, -1).add(location));

    			
    			defLeafSet.add(new Vector3i(-1, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(1, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, -1).add(location));
    			defLeafSet.add(new Vector3i(1, 0, 1).add(location));

    			
    			defLeafSet.add(new Vector3i(-1, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(1, -1, -1).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, -1).add(location));
    			defLeafSet.add(new Vector3i(1, -1, 1).add(location));
    			
    			defLeafSet.add(new Vector3i(1, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, -1, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, -1, 0).add(location));
    			defLeafSet.add(new Vector3i(0, -1, -1).add(location));
    		case 1:
    			defLeafSet.add(new Vector3i(0, 1, 0).add(location));
    			
    			defLeafSet.add(new Vector3i(1, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 0, 1).add(location));
    			defLeafSet.add(new Vector3i(-1, 0, 0).add(location));
    			defLeafSet.add(new Vector3i(0, 0, -1).add(location));
    			break;
    	}
    	return defLeafSet;
    }
}
