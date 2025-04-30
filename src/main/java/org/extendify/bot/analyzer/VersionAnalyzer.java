package org.extendify.bot.analyzer;

import org.extendify.bot.checker.VersionInfo;
import org.extendify.bot.util.OperatingSystem;

import java.util.List;
import java.util.Optional;

public class VersionAnalyzer {
    private final VersionScanner scanner;
    private final StringDiffAnalyzer diffAnalyzer;

    public VersionAnalyzer(List<VersionInfo> versions) {
        this.scanner = createScanner(versions);
        this.diffAnalyzer = createDiffAnalyzer(versions);
    }

    private static VersionScanner createScanner(List<VersionInfo> versions) {
        Optional<VersionInfo> version = versions.stream().filter(v -> v.getChannel().equals("MS STORE")).findFirst();
        return version.map(VersionScanner::new).orElse(null);
    }

    private static StringDiffAnalyzer createDiffAnalyzer(List<VersionInfo> versions) {
        Optional<VersionInfo> windowsVersion = versions.stream().filter(v -> v.getChannel().equals("MS STORE")).findFirst();
        Optional<VersionInfo> androidVersion = versions.stream().filter(v -> v.getOs().equals(OperatingSystem.ANDROID)).findFirst();
        return new StringDiffAnalyzer(windowsVersion.orElse(null), androidVersion.orElse(null));
    }

    public void runAnalyzer() {
        if (this.scanner != null) {
            this.scanner.startScanAsync();
        }

        if (this.diffAnalyzer != null) {
            this.diffAnalyzer.diffStringsAsync();
        }
    }
}
