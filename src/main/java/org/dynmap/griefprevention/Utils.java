package org.dynmap.griefprevention;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    public static String getFileExtension(Path path) {
        String name = path.getFileName().toString();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }

        return name.substring(lastIndexOf);
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.toPath());
    }

    public static String getNameWithoutExtension(Path path) {
        String name = path.getFileName().toString();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }

        return name.substring(0, lastIndexOf);
    }

    public static String getNameWithoutExtension(File file) {
        return getNameWithoutExtension(file.toPath());
    }

    public static Path addToFileName(Path path, String string) {
        return Paths.get(path.getParent().toString(), getNameWithoutExtension(path) + string + getFileExtension(path));
    }

    public static File addToFileName(File file, String string) {
        return addToFileName(file.toPath(), string).toFile();
    }
}
