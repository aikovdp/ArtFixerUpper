package me.aikovdp.artfixerupper.fixers;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegionFixer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionFixer.class);
    private final ItemFixer itemFixer;

    public RegionFixer(ItemFixer itemFixer) {
        this.itemFixer = itemFixer;
    }

    public void fixRegionDir(Path worldDir) {
        Path regionDir = worldDir.resolve("region");
        if (Files.notExists(regionDir)) {
            LOGGER.info("No region directory found, skipping...");
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionDir)) {
            int totalCounter = 0;
            int repairCounter = 0;
            int failCounter = 0;
            for (Path regionFile : stream) {
                totalCounter++;
                try {
                    if (fixRegionFile(regionFile.toFile())) {
                        repairCounter++;
                    }
                } catch (IOException e) {
                    failCounter++;
                    LOGGER.error("Error occurred while reading or writing to {}", regionFile, e);
                }
            }
            LOGGER.info("Successfully repaired {} out of {} chunk files, {} failed",
                    repairCounter, totalCounter, failCounter);
        } catch (IOException e) {
            LOGGER.warn("Unable to read playerdata", e);
        }
    }

    private boolean fixRegionFile(File regionFileFile) throws IOException {
        boolean dirty = false;
        try (RegionFile regionFile = new RegionFile(regionFileFile, regionFileFile.getParentFile(), true)) {
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    boolean changed = fixChunkInFile(regionFile, new ChunkPos(x, y));
                    if (!dirty && changed) {
                        dirty = true;
                    }
                }
            }
        }
        if (dirty) {
            LOGGER.info("Wrote to " + regionFileFile);
        }
        return dirty;
    }

    private boolean fixChunkInFile(RegionFile regionFile, ChunkPos pos) throws IOException {
        try (var dis = regionFile.getChunkDataInputStream(pos)) {
            if (dis == null) {
                return false;
            }
            CompoundBinaryTag chunk = BinaryTagIO.unlimitedReader().read((DataInput) dis);
            Optional<CompoundBinaryTag> fixedChunk = fixChunk(chunk);
            if (fixedChunk.isPresent()) {
                try (var dos = regionFile.getChunkDataOutputStream(pos)) {
                    BinaryTagIO.writer().write(fixedChunk.get(), (DataOutput) dos);
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<CompoundBinaryTag> fixChunk(CompoundBinaryTag chunk) {
        if (chunk.getInt("DataVersion") != 2586) {
            return Optional.empty(); // Never opened on 1.16.5, so data never got borked
        }
        CompoundBinaryTag level = chunk.getCompound("Level");
        AtomicBoolean tileEntitiesDirty = new AtomicBoolean(false);
        List<CompoundBinaryTag> tileEntities = level.getList("TileEntities", BinaryTagTypes.COMPOUND).stream()
                .map(CompoundBinaryTag.class::cast)
                .map(tileEntity -> {
                    ListBinaryTag originalItems = tileEntity.getList("Items", BinaryTagTypes.COMPOUND);
                    Optional<ListBinaryTag> fixedItems = itemFixer.fixItemList(originalItems);
                    if (fixedItems.isPresent()) {
                        tileEntitiesDirty.set(true);
                        return tileEntity.put("Items", fixedItems.get());
                    }
                    return tileEntity;
                })
                .toList();
        if (tileEntitiesDirty.get()) {
            level = level.put("TileEntities", ListBinaryTag.from(tileEntities));
        }

        AtomicBoolean entitiesDirty = new AtomicBoolean(false);
        List<CompoundBinaryTag> entities = level.getList("Entities", BinaryTagTypes.COMPOUND).stream()
                .map(CompoundBinaryTag.class::cast)
                .map(entity -> {
                    CompoundBinaryTag originalItem = entity.getCompound("Item");
                    Optional<CompoundBinaryTag> fixedItem = itemFixer.fixItem(originalItem);
                    if (fixedItem.isPresent()) {
                        entitiesDirty.set(true);
                        return entity.put("Item", fixedItem.get());
                    }
                    return entity;
                })
                .toList();
        if (entitiesDirty.get()) {
            level = level.put("Entities", ListBinaryTag.from(entities));
        }

        if (tileEntitiesDirty.get() || entitiesDirty.get()) {
            return Optional.of(chunk.put("Level", level));
        }
        return Optional.empty();
    }
}
