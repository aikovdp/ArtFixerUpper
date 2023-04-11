package me.aikovdp.artfixerupper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        ItemFix itemFix;
        try {
            itemFix = new ItemFix(Path.of("art.csv"));
        } catch (IOException e) {
            LOGGER.error("Unable to read art.csv", e);
            return;
        }
        PlayerFix playerFix = new PlayerFix(itemFix);
        playerFix.fixPlayerDataDir(worldDir);


        RegionFix regionFix = new RegionFix(itemFix);
        regionFix.fixRegionDir(worldDir);
    }
}
