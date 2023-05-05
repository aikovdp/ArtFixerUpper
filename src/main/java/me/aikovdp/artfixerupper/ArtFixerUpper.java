package me.aikovdp.artfixerupper;

import me.aikovdp.artfixerupper.fixers.ItemFixer;
import me.aikovdp.artfixerupper.fixers.MapDataFixer;
import me.aikovdp.artfixerupper.fixers.PlayerFixer;
import me.aikovdp.artfixerupper.fixers.RegionFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ArtFixerUpper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtFixerUpper.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.error("You did not provide a path");
            return;
        }
        Path worldDir = Path.of(args[0]);
        if (!Files.isDirectory(worldDir)) {
            LOGGER.error("Path does not lead to a directory");
            return;
        }
        Map<String, Integer> titleMap;
        try {
            titleMap = readArtCSV(Path.of("art.csv"));
        } catch (IOException e) {
            LOGGER.error("Unable to read art.csv", e);
            return;
        }

        MapDataFixer mapDataFixer = new MapDataFixer(titleMap);
        mapDataFixer.fixDataDir(worldDir);

        ItemFixer itemFixer = new ItemFixer(titleMap);

        PlayerFixer playerFixer = new PlayerFixer(itemFixer);
        playerFixer.fixPlayerDataDir(worldDir);

        RegionFixer regionFixer = new RegionFixer(itemFixer);
        regionFixer.fixRegionDir(worldDir);
    }

    private static Map<String, Integer> readArtCSV(Path path) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        Scanner scanner = new Scanner(path);
        scanner.useDelimiter("[,\n]");
        while (scanner.hasNextLine()) {
            map.put(scanner.next(), scanner.nextInt());
        }
        return Collections.unmodifiableMap(map);
    }
}
