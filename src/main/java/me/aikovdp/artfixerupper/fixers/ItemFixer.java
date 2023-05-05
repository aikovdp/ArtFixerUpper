package me.aikovdp.artfixerupper.fixers;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ItemFixer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemFixer.class);
    private final Map<String, Integer> titleMap;

    public ItemFixer(Map<String, Integer> titleMap) {
        this.titleMap = titleMap;
    }

    public Optional<ListBinaryTag> fixItemList(ListBinaryTag itemList) {
        if (itemList.elementType() != BinaryTagTypes.COMPOUND && itemList.size() != 0) {
            throw new IllegalArgumentException("Item list elements must be compound tags.");
        }
        AtomicBoolean dirty = new AtomicBoolean(false);
        List<CompoundBinaryTag> list = itemList.stream()
                .map(item -> (CompoundBinaryTag) item)
                .map(item -> {
                    Optional<CompoundBinaryTag> fixedItem = fixItem(item);
                    if (fixedItem.isPresent()) {
                        dirty.set(true);
                        return fixedItem.get();
                    }
                    return item;
                })
                .toList();
        return dirty.get() ? Optional.of(ListBinaryTag.from(list)) : Optional.empty();
    }

    public Optional<CompoundBinaryTag> fixItem(CompoundBinaryTag item) {
        if (item.getString("id").equals("minecraft:filled_map")) {
            return fixFilledMapItem(item);
        }
        if (item.getString("id").matches("minecraft:([a-z]+_)*shulker_box")) {
            return fixShulkerBoxItem(item);
        }
        return Optional.empty();
    }

    private Optional<CompoundBinaryTag> fixShulkerBoxItem(CompoundBinaryTag item) {
        CompoundBinaryTag tag = item.getCompound("tag");
        CompoundBinaryTag blockEntityTag = tag.getCompound("BlockEntityTag");
        Optional<ListBinaryTag> fixedContents = fixItemList(blockEntityTag.getList("Items", BinaryTagTypes.COMPOUND));
        if (fixedContents.isEmpty()) {
            return Optional.empty();
        }
        blockEntityTag = blockEntityTag.put("Items", fixedContents.get());
        tag = tag.put("BlockEntityTag", blockEntityTag);
        return Optional.of(item.put("tag", tag));
    }

    private Optional<CompoundBinaryTag> fixFilledMapItem(CompoundBinaryTag item) {
        CompoundBinaryTag tag = item.getCompound("tag");
        String itemName = tag.getCompound("display").getString("Name");
        if (itemName.isEmpty()) { // Not an artwork
            return Optional.empty();
        }
        String title = ((TextComponent) GsonComponentSerializer.gson().deserialize(itemName)).content();
        Integer id = titleMap.get(title);
        if (id == null || id == tag.getInt("map")) { // Title isn't in database or item is already correct
            return Optional.empty();
        }
        LOGGER.info("Replacing map item ID {} with id {}", tag.getInt("map"), id);
        tag = tag.putInt("map", id);
        return Optional.of(item.put("tag", tag));
    }


}
