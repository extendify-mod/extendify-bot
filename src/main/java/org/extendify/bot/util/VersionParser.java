package org.extendify.bot.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class VersionParser {
    private static final int PARTS_SIZE = 4;

    public static int[] parseVersion(String version) {
        int[] result = new int[PARTS_SIZE];
        String[] parts = version.split("\\.");
        for (int i = 0; i < PARTS_SIZE; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }

    public static CompareResult compare(String a, String b) {
        return compare(parseVersion(a), parseVersion(b));
    }

    public static CompareResult compare(int[] a, int[] b) {
        for (int i = 0; i < PARTS_SIZE; i++) {
            if (a[i] > b[i]) {
                return CompareResult.NEWER;
            } else if (a[i] < b[i]) {
                return CompareResult.OLDER;
            }
        }
        return CompareResult.EQUAL;
    }
}
