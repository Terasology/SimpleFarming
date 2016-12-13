Simple Farming
==============
**Simple Farming** is a module that adds capability to grow plants in Terasology. Using seeds that you acquire. After the plant is ripe, you can harvest the 'fruit' that is grown by that particular plant.

Farming
-------
1. Plant the seed in a ground.
2. Wait for plant to grow.
3. Wait for produce to ripe.
4. Harvest produce, you can do this by **pressing E** while facing the plant.
5. Destroy expired plant OR go back to step 3.

Growth Stage
-----------------
Every plant can have a varying amount of growth stages in their seed's prefab.
This prefab from TestberrySeed.prefab shows the growth stages of Testberry.

Items
-----
 - Un-Growth Can - Make the plant goes 1 stage back in their growth stage.
 - Growth Can - Make the plant goes 1 stage forward in their growth stage.

Making New Plants
-----------------
You can start making new plants by creating a new prefab file at assets/prefabs

*Usually,* for each plants you want to make this prefabs:
1. **Seed Prefab** (Ex: TestberrySeed.prefab)
2. **Fruit prefab** (Ex: Testberry.prefab)
3. **Ripe Stage of The Plant Prefab** (Ex: TestberrySeed.prefab)

You will also need to create a .block file for each stages of the plant at assets/blocks and the textures for each stages of the plant at blockTiles.

Seed Prefab
-----------
*For a good example, you should see TestberrySeed.prefab at assets/prefabs*

**maxRandom** & **fixed** sets the duration of that particular stage. The plant can grow to the next stage randomly during the range between maxRandom and fixed. If the value in fixed is reach. The plant immedietely grows to the next growth stage. This will make the plants to not grow to the next stage all at the exact time. Some plants will seems to grow faster than the other.

**:engine:halfblock** can be use to create a half-size block. This can be use if you are trying to characterize the plant being "small" in size during that particular growth stage. (Ex: In the first stage of TestBerry Plant, the plant is a half-block of berry bush block).

![A compharison between a half-block and a full-block](http://i.imgur.com/URLFbzo.png)

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

Fruit Prefab
-----------
*For a good example, you should see Testberry.prefab at assets/prefabs*

This prefab should be pretty intuitive. This prefab creates the fruit that will be drop when you harvest the plant that grows it.

Ripe Stage of The Plant Prefab
-----------
*For a good example, you should see TestberryBushFull.prefab at assets/prefabs*

This prefab basically manage what item are obtained when that particular plant is harvested. 

Block Files
-------
*For a good example, you should see the .block files on assets/blocks*

The block files basically sets the characteristic for the plant block. 
You can use **basedOn** to make that block have the characteristic of the block that it is based on. 

**tile** should show where the texture for that particular block is located.

One thing that you need to pay attention when you want to create a stage that can be harvested is adding this code to the .block file. (Ex: TestberryBushFull.block)

       "entity": {
        "prefab": "<Ripe stage of the plant prefab>"
         },
This code will allow the game to know that this particular block is associated with a prefab that indicated that this stage is where the fruit is ripe and is ready to be harvested.

Produce Mechanisms
------------------
There's 2 kind of produce mechanism that you can make using the SimpleFarming module.
 1. Periodic
 2. Once
A **Periodic** produce will grow again after it's harvest. On the other hand, a **Once** produce will only be a one-time harvest and after you harvest it, you need to plant it again from the seed.

To make a plant have a **periodic** produce mechanism, you need to add   `"UnGrowPlantOnHarvest": {},` in ripe stage of the plant prefab file, and vice versa.

Destroying a plant with produce, will also yield a produce.
