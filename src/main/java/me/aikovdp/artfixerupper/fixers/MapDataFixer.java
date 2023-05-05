package me.aikovdp.artfixerupper.fixers;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class MapDataFixer {
    public static final long UUID_LEAST = -8082271879484814746L;
    public static final long UUID_MOST = 8674450473152037831L;
    public static final UUID UUID = new UUID(UUID_MOST, UUID_LEAST);
    private static final String ARTMAP_DIMENSION = "minecraft:minigames";
    private static final Logger LOGGER = LoggerFactory.getLogger(MapDataFixer.class);
    private final BinaryTagIO.Reader reader = BinaryTagIO.reader();
    private final BinaryTagIO.Writer writer = BinaryTagIO.writer();
    private final Map<String, Integer> titleMap;

    public MapDataFixer(Map<String, Integer> titleMap) {
        this.titleMap = titleMap;
    }

    public void fixDataDir(Path worldDir) {
        Path dataDir = worldDir.resolve("data");
        if (Files.notExists(dataDir)) {
            LOGGER.info("No data directory found, skipping...");
            return;
        }
        try (var stream = Files.newDirectoryStream(
                dataDir,
                path -> path.getFileName().toString().matches("map_\\d+\\.dat")
        )) {
            int totalCounter = 0;
            int repairCounter = 0;
            int failCounter = 0;
            for (Path path : stream) {
                totalCounter++;
                try {
                    if (fixMapDataFile(path)) {
                        repairCounter++;
                    }
                } catch (IOException e) {
                    failCounter++;
                    LOGGER.error("Error occurred while reading or writing to {}", path, e);
                }
            }
            LOGGER.info("Successfully repaired {} out of {} map data files, {} failed",
                    repairCounter, totalCounter, failCounter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean fixMapDataFile(Path path) throws IOException {
        int id = getMapIdFromPath(path);
        if (!titleMap.containsValue(id)) {
            return false;
        }
        CompoundBinaryTag root = reader.read(path, BinaryTagIO.Compression.GZIP);
        CompoundBinaryTag data = root.getCompound("data");
        boolean dirty = false;

        // Fix dimension
        String dimension = data.getString("dimension");
        if (!dimension.equals(ARTMAP_DIMENSION)) {
            data = data.putString("dimension", ARTMAP_DIMENSION);
            LOGGER.info("Map {}'s dimension {} will be changed to {}", id, dimension, ARTMAP_DIMENSION);
            dirty = true;
        } else {
            LOGGER.info("Map {}'s dimension {} is correct", id, dimension);
        }

        // Fix UUID
        long UUIDMost = data.getLong("UUIDMost");
        long UUIDLeast = data.getLong("UUIDLeast");
        if (UUIDLeast != UUID_LEAST || UUIDMost != UUID_MOST) {
            LOGGER.info("Map {}'s UUID {} will be changed to {}", id, new UUID(UUIDMost, UUIDLeast), UUID);
            data = data.putLong("UUIDMost", UUID_MOST);
            data = data.putLong("UUIDLeast", UUID_LEAST);
            dirty = true;
        } else {
            LOGGER.info("Map {}'s UUID {} is correct", id, new UUID(UUIDMost, UUIDLeast));
        }
        if (dirty) {
            writer.write(root.put("data", data), path, BinaryTagIO.Compression.GZIP);
            return true;
        }
        return false;
    }

    private static int getMapIdFromPath(Path path) {
        String fileName = path.getFileName().toString();
        return Integer.parseInt(fileName.substring("map_".length(), fileName.length() - ".dat".length()));
    }
}
