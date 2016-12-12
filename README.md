Simple Farming
==============
**Simple Farming** is a module that adds the capability to grow plants in Terasology using seeds. After the plant is ripe, you can harvest the 'fruit' that is grown by that particular plant.

Farming
-------
1. Plant the seed in a ground.
2. Wait for the plant to grow.
3. Wait for the produce to ripe.
4. Harvest the produce - you can do this by **pressing E** while facing the plant.
5. Destroy expired plant OR go back to step 3.

Items
-----
 - Un-Growth Can - Make the plant go 1 growth stage back.
 - Growth Can - Make the plant go 1 growth stage forward.

Creating New Plants
-----------------
You can start making new plants by creating a new prefab file at assets/prefabs.

Usually for each plant you want to make these three prefabs:
1. **Seed Prefab** (e.g. TestberrySeed.prefab)
2. **Fruit prefab** (e.g. Testberry.prefab)
3. **Ripe Stage of The Plant Prefab** (e.g. TestberrySeed.prefab)

You will also need to create a .block file for each stage of the plant at assets/block, as well as the textures for each stage of the plant at blockTiles.

Seed Prefab
-----------
*For a good example, see TestberrySeed.prefab at assets/prefabs*

`maxRandom` & `fixed` set the duration of a particular stage. The plant can grow to the next stage randomly during the range between maxRandom and fixed. If the value in `fixed` is reached, the plant immediately grows to the next growth stage. This will make the plants not grow to the next stage all at the exact time - some will get there faster than others!

`engine:halfblock` can be used to create a half-size block. This can be useful if you are trying to characterize the plant being "small" in size during that particular growth stage. (For instance, in the first stage of TestBerry Plant, the plant is a half-block of berry bush block).

![enter image description here](http://i.imgur.com/URLFbzo.png)

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

Fruit Prefab
-----------
*For a good example, see Testberry.prefab at assets/prefabs*

This prefab should be pretty intuitive. It creates the fruit that will be dropped when you harvest the plant that grows it.

Ripe Stage of The Plant Prefab
-----------
*For a good example, see TestberryBushFull.prefab at assets/prefabs*

This prefab manages the items that are are obtained when a particular plant is harvested. 

Block Files
-------
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

#Produce Mechanisms
- Periodic
- Once

#Other
- Destroying plant with produce also drops produce
