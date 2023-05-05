# ArtFixerUpper

ArtFixerUpper is a small program written for [Rokucraft](https://www.rokucraft.com) to fix corrupted data related
to [ArtMap#218](https://gitlab.com/BlockStack/ArtMap/-/issues/218). It aims to automatically repair map data, and edit
map items in region files and player data to revert overwritten IDs.

## Running

Before running the application, create a CSV dump of ArtMap's `Art.db` in the repository root, in the following format.

```csv
title,id
otherTitle,otherId
```

Then, edit `ARTMAP_DIMENSION`, `UUID_LEAST` and `UUID_MOST` in the `MapDataFixer` class to match the values you want to
be written to the map data files. `ARTMAP_DIMENSION` should be the namespaced ID of the world you configured in
ArtMap. `UUID_LEAST` and `UUID_MOST` refer to the least and most significant bits of that world's UUID respectively.

Finally, you can run the application with the following command:

```sh
./gradlew run --args="path/to/world"
```