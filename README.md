# Simple Farming

**Simple Farming** is a module that adds the capability to grow plants in Terasology using seeds. After the plant is ripe, you can harvest the 'fruit' that is grown by that particular plant.

## Farming

1. Plant the seed in a ground.
2. Wait for the plant to grow.
3. Wait for the produce to ripen.
4. Harvest the produce - you can do this by **pressing E** while facing the plant.
5. Destroy the expired plant OR go back to step 3.

## Items

 - Un-Growth Can - Make the plant go 1 growth stage back.
 - Growth Can - Make the plant go 1 growth stage forward.

## Creating New Plants

You can start making new plants by creating a new prefab file at assets/prefabs.

Usually for each plant you want to make these three prefabs:  
1. **Seed Prefab** (e.g. TestberrySeed.prefab)  
2. **Ripe Stage Prefab** (e.g. TestberryBushFull.prefab)
2. **Fruit prefab** (e.g. Testberry.prefab)  

You will also need to create a .block file for each stage of the plant at assets/blocks, as well as the textures for each stage of the plant at assets/blockTiles.

### Seed Prefab

*For a good example, see TestberrySeed.prefab at assets/prefabs*

`maxRandom` & `fixed` set the duration of a particular stage. The plant can grow to the next stage randomly during the range between maxRandom and fixed. If the value in `fixed` is reached, the plant immediately grows to the next growth stage. This will make the plants not grow to the next stage all at the exact time - some will get there faster than others!

`engine:halfblock` can be used to create a half-size block. This can be useful if you are trying to characterize the plant being "small" in size during that particular growth stage. (For instance, in the first stage of TestBerry Plant, the plant is a half-block of berry bush block).

![A comparison between a half-block and a full-block](http://i.imgur.com/URLFbzo.png)

```json
    "growthStages": {
        "SimpleFarming:BerryBush:engine:halfblock": {
        "maxRandom": 5000,
        "fixed": 10000
    },
    "SimpleFarming:BerryBush": {
        "maxRandom": 5000,
        "fixed": 10000
    },
    "SimpleFarming:MatureBerryBush": {
        "maxRandom": 5000,
        "fixed": 10000
    },
    "SimpleFarming:TestberryBushFull": {
        "maxRandom": 5000,
        "fixed": 10000
    }
}
```

### Ripe Stage Prefab

*For a good example, see TestberryBushFull.prefab at assets/prefabs*

This prefab manages the items that are are obtained when a particular plant is harvested. 

### Fruit Prefab

*For a good example, see Testberry.prefab at assets/prefabs*

This prefab should be pretty intuitive. It creates the fruit that will be dropped when you harvest the plant that grows it.

### Block Files

*For a good example, see the .block files on assets/blocks*

The block files sets the characteristics for the plant block. 
You can use `basedOn` to make that block have the characteristic of the block that it is based on. 

`tile` should show where the texture for that particular block is located.

One thing that you need to pay attention when you want to create a stage that can be harvested is adding this code to the .block file. (Ex: TestberryBushFull.block)

```json
"entity": {
    "prefab": "<Ripe stage of the plant prefab>"
}
```

This code will allow the game to know that this particular block is associated with a prefab that indicated that this stage is where the fruit is ripe and is ready to be harvested.

## Creating New Trees

Just like above, trees need to contain seed prefabs as well. However, to define a new tree, a few additional steps are required.

To indicate that a plant will become a tree, you will need to do so inside the seed prefab by setting `"growsIntoTree" : true`.

After the tree has gone through the growth stages specified under `PlantDefinition` in the seed prefab, it will grow into a tree. The properties of this tree will have to be specified under `TreeDefinition` in the seed prefab. 

```javascript
    "TreeDefinition": {
        "fruitName": "Peach Fruit", // the name of the fruit
        "fruitGrowthStages": {
            "SimpleFarming:PeachFruit": {
                "maxRandom": 5000,
                "fixed": 5000
            },
            "SimpleFarming:PeachFruitMature": {
                "maxRandom": 5000,
                "fixed": 5000
            },
            "SimpleFarming:PeachFruitFull": {
                "maxRandom": 6000,
                "fixed": 10000
            }
        },
        "trunkHeight": 5, // the height of the trunk in blocks
        "trunkBlock": "Core:PineTrunk", // the block that makes up the trunk
        "canopyLayers": [0, 0, 0, 3, 2, 1]
    }
```
This is an example definition of a tree definition. 

The fruit growth stages are similar to plant growth stages, in that they define the changes that will occur to the leaf blocks over time. Ideally the first stage should be an empty leaf block, while the last stage would contain the block with the fruit to be harvested.

The canopy layers define how the leaves will form on the tree. Each number corresponds to a higher layer of the tree, starting from the base of the trunk. It defines how large the canopy will be for that layer based on the number of blocks from the center.

0 would mean no leaves, 1 would mean a 1x1 region of leaves (1 block from the center), and 2 would mean a 3x3 region of leaves (2 blocks from the center). 

For instance, the above prefab would generate the following tree.

![](http://i.imgur.com/5hiraWw.png)

## Produce Mechanisms

There's 2 kind of produce mechanism that you can specify using the SimpleFarming module.
 1. Periodic
 2. Once

A **Periodic** produce will grow again after it's harvest. On the other hand, a **Once** produce will only be a one-time harvest and after you harvest it, you need to plant it again from the seed.

To make a plant have a **periodic** produce mechanism, you need to add `"UnGrowPlantOnHarvest": {},` in ripe stage of the plant prefab file, and vice versa.

Destroying a plant with produce, will also yield a produce.

## Credits for Images:

* Blueberry: Made by TheJYKoder (Original)
* BlueberryBushFull: Made by TheJYKoder (Original)
* Strawberry: Made by Patrick Wang (Original)
* Raspberry: Made by SufurElite (Original)
* Gojiberry: Made by Harry Wang (Original)
* Apple: http://opengameart.org/content/good-fruits-m484-games
* Apple Seed: Made by gkaretka (Original)
* Apple Bush Full: Made by gkaretka (Merge of BerryBushFull and Apple)
* Immature, Mature, and Ripe Pineapple: Made by mdj117 (Original)
* Pineapple Fruit: Made by mdj117 (Original)
* Pineapple: https://pixabay.com/en/pineapple-fruit-exotic-tropical-1916996/
* Pineapple Sapling : https://pixabay.com/en/chestnuts-conker-marron-nuts-seeds-151927/
* Watermelon: http://www.freestockphotos.biz/stockphoto/14464
* Watermelon Seed: Made by smsunarto (Original)
* Blackberry: Made by VaibhavBajaj (inspired by Testberry)
* BlackberrySeed: Made by VaibhavBajaj (inspired by Testberry)
* BlackberryBush: Made by VaibhavBajaj (inspired by Testberry)
* MatureBlackberryBush: Made by VaibhavBajaj (inspired by Testberry)
* BlackberryBushFull: Made by VaibhavBajaj (inspired by Testberry)
* Cranberry and all related: [András Ottó Földes](https://github.com/andriii25)
* Peach Tree Saplings, Fruits: iojw (Fruits modified from GreenLeaf)
* Peach: https://pixabay.com/en/peach-fruit-nectarine-plant-nature-41169/
* Peach Seed: https://pixabay.com/en/peach-nectarine-fruit-food-sweet-42902/
