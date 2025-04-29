package org.extendify.bot.analyzer;

import org.extendify.bot.checker.VersionInfo;
import org.extendify.bot.util.OperatingSystem;

import java.util.List;
import java.util.Optional;

public class VersionAnalyzer {
    private final List<VersionInfo> versions;
    private final VersionScanner scanner;
    private final TranslationDiffAnalyzer diffAnalyzer;

    public VersionAnalyzer(List<VersionInfo> versions) {
        this.versions = versions;
        this.scanner = createScanner(versions);
        this.diffAnalyzer = createDiffAnalyzer(versions);
    }

    private static VersionScanner createScanner(List<VersionInfo> versions) {
        Optional<VersionInfo> version = versions.stream().filter(v -> v.getUrl().endsWith(".appx")).findFirst();
        return version.map(VersionScanner::new).orElse(null);
    }

    private static TranslationDiffAnalyzer createDiffAnalyzer(List<VersionInfo> versions) {
        Optional<VersionInfo> windowsVersion = versions.stream().filter(v -> v.getUrl().endsWith(".appx")).findFirst();
        Optional<VersionInfo> androidVersion = versions.stream().filter(v -> v.getOs().equals(OperatingSystem.ANDROID)).findFirst();
        return new TranslationDiffAnalyzer(windowsVersion.orElse(null), androidVersion.orElse(null));
    }

    public void runAnalyzer() {
        if (this.scanner != null) {
            this.scanner.startScanAsync();
        }

        if (this.diffAnalyzer != null) {
            this.diffAnalyzer.diffTranslations();
        }

        for (VersionInfo version : this.versions) {
            version.deleteFile();
        }
    }
}
