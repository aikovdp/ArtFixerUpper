package me.aikovdp.artfixerupper;

import net.kyori.adventure.nbt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PlayerFix {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerFix.class);
    private final ItemFix itemFix;

    public PlayerFix(ItemFix itemFix) {
        this.itemFix = itemFix;
    }

    public void fixPlayerDataDir(Path worldDir) {
        Path playerDataDir = worldDir.resolve("playerdata");
        if (Files.notExists(playerDataDir)) {
            LOGGER.info("No playerdata directory found, skipping...");
            return;
        }
        try (
                DirectoryStream<Path> stream = Files.newDirectoryStream(
                        playerDataDir,
                        path -> path.getFileName().toString() // Filename must follow playerdata naming scheme
                                .matches("[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}\\.dat")
                )
        ) {
            int totalCounter = 0;
            int repairCounter = 0;
            int failCounter = 0;
            for (Path playerData : stream) {
                totalCounter++;
                try {
                    if (fixPlayerDataFile(playerData)) {
                        repairCounter++;
                    }
                } catch (IOException e) {
                    failCounter++;
                    LOGGER.error("Error occurred while reading or writing to {}", playerData, e);
                }
            }
            LOGGER.info("Successfully repaired {} out of {} playerdata files, {} failed",
                    repairCounter, totalCounter, failCounter);
        } catch (IOException e) {
            LOGGER.warn("Unable to read playerdata", e);
        }
    }

    private boolean fixPlayerDataFile(Path path) throws IOException {
        CompoundBinaryTag player = BinaryTagIO.unlimitedReader().read(path, BinaryTagIO.Compression.GZIP);
        if (player.getInt("DataVersion") != 2586) {
            return false; // Never opened on 1.16.5, so data never got borked
        }
        boolean dirty = false;
        Optional<ListBinaryTag> inventory = itemFix.fixItemList(player.getList("Inventory", BinaryTagTypes.COMPOUND));
        if (inventory.isPresent()) {
            player = player.put("Inventory", inventory.get());
            dirty = true;
        }
        Optional<ListBinaryTag> enderItems = itemFix.fixItemList(player.getList("EnderItems", BinaryTagTypes.COMPOUND));
        if (enderItems.isPresent()) {
            player = player.put("EnderItems", enderItems.get());
            dirty = true;
        }
        if (dirty) {
            BinaryTagIO.writer().write(player, path, BinaryTagIO.Compression.GZIP);
        }
        return dirty;
    }
}
