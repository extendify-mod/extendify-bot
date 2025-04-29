package org.extendify.bot.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScannablePlatform {
    WINDOWS_X86_64(
            "win32-x86_64/spotify_installer-%s-%s.exe",
            OperatingSystem.WINDOWS,
            "x86/x64",
            System.getProperty("WINDOWS_RELEASE_CHANNEL"),
            System.getProperty("WINDOWS_PING_ROLE")
    ),
    WINDOWS_ARM64(
            "win32-arm64/spotify_installer-%s-%s.exe",
            OperatingSystem.WINDOWS,
            "arm64",
            System.getProperty("WINDOWS_RELEASE_CHANNEL"),
            System.getProperty("WINDOWS_PING_ROLE")
    ),
    MACOS_X86_64(
            "osx-x86_64/spotify-autoupdate-%s-%s.tbz",
            OperatingSystem.MACOS,
            "x86/x64",
            System.getProperty("MACOS_RELEASE_CHANNEL"),
            System.getProperty("MACOS_PING_ROLE")
    ),
    MACOS_ARM64(
            "osx-arm64/spotify-autoupdate-%s-%s.tbz",
            OperatingSystem.MACOS,
            "arm64",
            System.getProperty("MACOS_RELEASE_CHANNEL"),
            System.getProperty("MACOS_PING_ROLE")
    );

    private final String path;
    private final OperatingSystem os;
    private final String architecture;
    private final String channelId;
    private final String roleId;
}
