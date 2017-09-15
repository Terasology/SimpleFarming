# Simple Farming

**Simple Farming** is a module that adds the ability to grow & harvest edible plants.
The module currently supports both bushes and vines, which allow for different plant types.
Trees are currently not added and bushes are used in their place.

## Quick Guide

Once you have a seed for the item, right click a free patch of ground to plant it.
The plant will grow and eventually bear produce. Once this happens you can harvest the produce by interacting with it (default key of E)
The plant will then either be removed or revert back and begin to grow more produce.

If you break the plant before the fruit is ready no seeds will drop, so ensure you only break the plant when it is finished.

## Modding Quick Guide

The full details of the included prefabs & components is on the wiki.

Each plant will have two or three prefabs. One for the plant itself and one or two for the seed & the produce. The seed & the produce can be the same item, in the case of the Carrot.
The seed prefab must have a SeedDefinition which contains the name of the plant prefab.
The plant prefab will be a BushDefinition or a VineDefinition depending on the type of plant.
The bush definition will contain a list of blocks & times for each stage as well as an the produce to create on harvest.
A vine definition will contain a block to use as a stem and a prefab to use as a bud. This bud prefab will be a bush.


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
* Normal, Mature and Full Orange Bush: Created from Cranberry and Apple bushes by Jellysnake
* Orange & Orange Seeds: Created by Jellysnake
* Grape images: Made by BenjaminAmos (Original)
* Pear: https://pixabay.com/en/fruit-owocostan-356522/
* PearSeed: Made by Thuckerz (Original)
* PearBush: Made by Thuckerz (Original)
* MaturePearBush: Made by Thuckerz (Original)
* PearBushFull: Made by Thuckerz (Original)
* Grapes: https://commons.wikimedia.org/wiki/File:Twemoji_1f347.svg (Resized)
* GrapesSeed: https://pixabay.com/en/dried-leaf-plant-natural-leaves-774823/ (Resized)
* GrapesBush :https://pixabay.com/en/grape-fruit-fruit-tree-green-1511280/ (Resized)
* GrapesBushFull :https://pixabay.com/en/grape-fruit-fruit-tree-green-1511280/ (Resized) (Edited by DhananjayGarg)
* Peach Tree Saplings, Fruits: iojw (Fruits modified from GreenLeaf)
* Peach: https://pixabay.com/en/peach-fruit-nectarine-plant-nature-41169/
* Peach Seed: https://pixabay.com/en/peach-nectarine-fruit-food-sweet-42902/
* Melon: Mandar Juvekar (original)
* Melon Vine Seed: Mandar Juvekar (original)
* Melon Vine Trunk: Mandar Juvekar (original)
* Pumpkin: Mandar Juvekar (original)
* Pumpkin Vine Seed: Mandar Juvekar (original)
* Pumpkin Vine Sapling: Mandar Juvekar (modified version of BerryBush)
* Pumpkin Vine Trunk: Mandar Juvekar (original)
* Cherry: https://www.pexels.com/photo/fruit-cherries-109274/
* CherrySeed: https://www.pexels.com/photo/close-up-of-autumn-leaves-over-white-background-255073/
* CherryBushFull: https://www.pexels.com/photo/nature-red-shrub-trees-1895/
* MatureCherryBush: https://www.pexels.com/photo/red-cherry-fruit-on-brown-tree-branch-63312/
* Tomato: SufurElite (original)
* Tomato Vine Seed: SufurElite based on Mandar Juvekar's original
* Tomato Vine Stem: SufurElite based on Mandar Juvekar's original
* PassionFruit: mdj117 (original)
* PassionFruit seed: mdj117 (original)
* PassionFruit trunk: mdj117 (original)
* PassionFruit Sapling: mdj117 (original)
* Cucumber and all related: [András Ottó Földes](https://github.com/andriii25) (Trunk based on Melon Vine Trunk)
* Squash : Made by Dhananjay Garg
* SquashVineSapling : Made by Dhananjay Garg
* SquashVineTrunk : Made by Dhananjay Garg
* SquashVineSeed : Made by Dhananjay Garg
* Guava: https://upload.wikimedia.org/wikipedia/commons/d/d4/A_aesthetic_guava_fruits.JPG (Resized)
* GuavaSeed : https://pixabay.com/en/seeds-shells-yellow-foods-nuts-576562/ (Resized)
* GuavaBush : https://pixabay.com/en/apple-branch-deciduous-fruit-1300027/ (Resized)
* GuavaBushFull : https://pixabay.com/en/apple-branch-deciduous-fruit-1300027/ (Resized) (Edited by Jay)
