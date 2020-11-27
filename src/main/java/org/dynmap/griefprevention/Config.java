package org.dynmap.griefprevention;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@ConfigSerializable
public class Config{

    static final TypeToken<Config> TYPE = new TypeToken<Config>() {};

    private static final String DEF_INFOWINDOW = "<div class=\"infowindow\">"
            + "Name: <span style=\"font-weight:bold;\">%claimname%</span><br/>"
            + "Owner: <span style=\"font-weight:bold;\">%owner%</span><br/>"
            + "Type: <span style=\"font-weight:bold;\">%gptype%</span><br/>"
            + "Last Seen: <span style=\"font-weight:bold;\">%lastseen%</span><br/>"
            + "Permission Trust: <span style=\"font-weight:bold;\">%managers%</span><br/>"
            + "Trust: <span style=\"font-weight:bold;\">%builders%</span><br/>"
            + "Container Trust: <span style=\"font-weight:bold;\">%containers%</span><br/>"
            + "Access Trust: <span style=\"font-weight:bold;\">%accessors%</span></div>";

    private static final String DEF_ADMININFOWINDOW = "<div class=\"infowindow\">"
            + "<span style=\"font-weight:bold;\">Administrator Claim</span><br/>"
            + "Permission Trust: <span style=\"font-weight:bold;\">%managers%</span><br/>"
            + "Trust: <span style=\"font-weight:bold;\">%builders%</span><br/>"
            + "Container Trust: <span style=\"font-weight:bold;\">%containers%</span><br/>"
            + "Access Trust: <span style=\"font-weight:bold;\">%accessors%</span></div>";

    private static final String ADMIN_ID = "administrator";

    public Config() {}

    public static Config generateErrorDefault(Path path) {
        if(Files.exists(path)) {
            DateFormat dateFormat = new SimpleDateFormat("-yyyy-MM-ddTHH.mm.ss");
            Path newPath = Utils.addToFileName(path, dateFormat.format(new Date()) + ".errored");
            try {
                Files.move(path, newPath);
                DynmapGriefprevention.getInstance().logger.warn("Old config renamed to \"" + newPath.getFileName() + "\".");
            } catch (IOException e) {
                DynmapGriefprevention.getInstance().logger.error("Failed to backup errored config", e);
            }
        }
        return new Config();
    }

    @Setting("layerName")
    String layerName = "GriefPrevention";

    @Setting("layerMinZoom")
    int layerMinZoom = 0;

    @Setting("layerPriority")
    int layerPriority = 10;

    @Setting("layerHideByDefault")
    boolean layerHideByDefault = false;

    @Setting("use3dRegions")
    boolean use3d = false;

    @Setting("claimInfoWindow")
    String infoWindow = DEF_INFOWINDOW;

    @Setting("adminClaimInfoWindow")
    String adminInfoWindow = DEF_ADMININFOWINDOW;

    @Setting("maxDepth")
    int maxDepth = 16;

    @Setting("regionStyle")
    AreaStyle regionStyle = new AreaStyle();

    @Setting("ownerStyle")
    Map<String, AreaStyle> ownerStyle = new HashMap<>();

    @Setting(value="visibleRegions", comment="A whitelist of regions to display")
    HashSet<String> visibleRegions = new HashSet<>();

    @Setting(value="hiddenRegions", comment="A blacklist of regions not to display")
    HashSet<String> hiddenRegions = new HashSet<>();

    @Setting("updatePeriod")
    int updatePeriod = 300;
}