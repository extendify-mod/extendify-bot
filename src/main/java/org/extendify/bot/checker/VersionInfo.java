package org.extendify.bot.checker;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.extendify.bot.util.OperatingSystem;
import org.extendify.bot.util.ScannablePlatform;

@Getter
@Builder
@ToString
public class VersionInfo {
    private OperatingSystem os;
    private String architecture;
    private String channel;
    private String version;
    private String url;

    public boolean matches(VersionInfo other) {
        return this.os.equals(other.os) && this.architecture.equals(other.architecture) && this.channel.equals(other.channel);
    }

    public boolean matches(ScannablePlatform other) {
        return this.os.equals(other.getOs()) && this.architecture.equals(other.getArchitecture());
    }
}
