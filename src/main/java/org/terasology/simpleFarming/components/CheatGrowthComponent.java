// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.simpleFarming.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class CheatGrowthComponent implements Component<CheatGrowthComponent> {
    public boolean causesUnGrowth = false;

    @Override
    public void copy(CheatGrowthComponent other) {
        this.causesUnGrowth = other.causesUnGrowth;
    }
}
