package org.extendify.bot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pink.madis.apk.arsc.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Test {
    private static final Logger LOGGER = LogManager.getLogger("Test");

    public static void main(String[] args) {
        try {
            Path xapkPath = Paths.get("./data/android_anycpu_apkpure");
            ZipFile archive = new ZipFile(xapkPath.toFile());

            ZipEntry arscEntry = archive.getEntry("resources.arsc");
            if (arscEntry != null) {
                ResourceFile resources = ResourceFile.fromInputStream(archive.getInputStream(arscEntry));
                ResourceTableChunk table = (ResourceTableChunk) resources.getChunks().get(0);
                PackageChunk pkg = table.getPackages().iterator().next();

                for (TypeChunk typeChunk : pkg.getTypeChunks()) {
                    if (!typeChunk.getTypeName().equals("string")) {
                        continue;
                    }

                    if (!new String(typeChunk.getConfiguration().language()).equals("en")) {
                        continue;
                    }

                    for (TypeChunk.Entry entry : typeChunk.getEntries().values()) {
                        ResourceValue value = entry.value();
                        if (value == null || value.data() > table.getStringPool().getStringCount()) {
                            continue;
                        }

                        LOGGER.info(
                                "{}: {}",
                                entry.key(),
                                table.getStringPool().getString(value.data())
                        );
                    }
                }
            }

            archive.close();
        } catch (IOException e) {
            LOGGER.error("Error while reading Android translations", e);
        }
    }
}
