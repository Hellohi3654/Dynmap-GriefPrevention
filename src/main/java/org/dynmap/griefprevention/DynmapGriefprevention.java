package org.dynmap.griefprevention;

import com.google.inject.Inject;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.GriefPreventionApi;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.api.event.ChangeClaimEvent;
import me.ryanhamshire.griefprevention.api.event.CreateClaimEvent;
import me.ryanhamshire.griefprevention.api.event.DeleteClaimEvent;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.griefprevention.commands.DynmapGriefpreventionCommand;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Plugin(
        id = "dynmapgriefprevention",
        name = "Dynmap",
        dependencies = { @Dependency(id = "dynmap"), @Dependency(id = "griefprevention")}
)
public class DynmapGriefprevention {

    private static DynmapGriefprevention instance;

    @Inject
    public Logger logger;

    private static final String ADMIN_ID = "administrator";
    private boolean reload;

    private DynmapCommonAPI dynmap;
    private MarkerAPI markerapi;
    private GriefPreventionApi gp;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;

    private static Config config;

    private MarkerSet markerSet;

    private Map<String, AreaMarker> markersById = new HashMap<>();

    private boolean disablePlugin;

    public DynmapGriefprevention() {
        instance = this;
    }

    public static DynmapGriefprevention getInstance() {
        return instance;
    }

    @Listener
    //Finish any work needed in order to be functional. Global event handlers should get registered in this stage.
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this, DynmapGriefpreventionCommand.getCommandSpec(), "dynmapgriefprevention", "dynmapgp", "dyngp", "dmgp", "dgp");

        logger.info("Registered commands.");
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Initializing...");

        gp = GriefPrevention.getApi();

        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI api) {
                dynmap = api;
                markerapi = api.getMarkerAPI();

                activate();
            }
        });
    }

    private class GriefPreventionUpdate implements Consumer<Task> {

        @Override
        public void accept(Task task) {
            if(!disablePlugin) {
                updateClaims();
            } else {
                task.cancel();
            }
        }
    }

    private String formatInfoWindow(Claim claim, AreaMarker m) {
        String v;
        if(claim.isAdminClaim())
            v = "<div class=\"regioninfo\">"+config.adminInfoWindow+"</div>";
        else
            v = "<div class=\"regioninfo\">"+config.infoWindow+"</div>";
        v = v.replace("%owner%", claim.isAdminClaim() ? ADMIN_ID : claim.getOwnerName().toPlain())
            .replace("%area%", Integer.toString(claim.getArea()))
            .replace("%claimname%", claim.getData().getName().isPresent() ? claim.getName().get().toPlain() : "No Name Set")
            .replace("%lastseen%", claim.getData().getDateLastActive().toString())
            .replace("%gptype%", claim.getType().toString());

        List<String> builders = claim.getGroupTrusts(TrustType.BUILDER);
        List<String> containers = claim.getGroupTrusts(TrustType.CONTAINER);
        List<String> accessors = claim.getGroupTrusts(TrustType.ACCESSOR);
        List<String> managers = claim.getGroupTrusts(TrustType.MANAGER);

        /* Build builders list */
        StringBuilder strBuilder = new StringBuilder();
        for(int i = 0; i < builders.size(); i++) {
            if(i > 0) strBuilder.append(", ");
            strBuilder.append(builders.get(i));
        }
        v = v.replace("%builders%", strBuilder.toString());
        /* Build containers list */
        strBuilder = new StringBuilder();
        for(int i = 0; i < containers.size(); i++) {
            if(i > 0) strBuilder.append(", ");
            strBuilder.append(containers.get(i));
        }
        v = v.replace("%containers%", strBuilder.toString());
        /* Build accessors list */
        strBuilder = new StringBuilder();
        for(int i = 0; i < accessors.size(); i++) {
            if(i > 0) strBuilder.append(", ");
            strBuilder.append(accessors.get(i));
        }
        v = v.replace("%accessors%", strBuilder.toString());
        /* Build managers list */
        strBuilder = new StringBuilder();
        for(int i = 0; i < managers.size(); i++) {
            if(i > 0) strBuilder.append(", ");
            strBuilder.append(managers.get(i));
        }
        return v.replace("%managers%", strBuilder.toString());
    }

    private boolean isVisible(String owner, String worldName, String uuid) {
        HashSet<String> visible = config.visibleRegions;
        HashSet<String> hidden = config.hiddenRegions;

        // Check the whitelist, if it's set
        if((visible != null) && (visible.size() > 0)) {
            if(!visible.contains(owner) && !visible.contains("world:" + worldName) &&
                    !visible.contains(worldName + "/" + owner) && !visible.contains(uuid)) {
                return false;
            }
        }
        // Check the blacklist, if it's set
        if((hidden != null) && (hidden.size() > 0)) {
            return !(hidden.contains(owner) || hidden.contains("world:" + worldName) || hidden.contains(worldName + "/" + owner) || hidden.contains(uuid));
        }
        return true;
    }

    private void addStyle(String owner, String worldId, AreaMarker marker, Claim claim) {
        AreaStyle areaStyle = null;

        if(!config.ownerStyle.isEmpty()) {
            areaStyle = config.ownerStyle.get(owner.toLowerCase());
        }

        if(areaStyle == null)
            areaStyle = config.regionStyle;

        int strokeColor, fillColor;

        switch(claim.getType()) {
            case BASIC: // Yellow
                strokeColor = 0xFFFF00;
                fillColor = 0xFFFF00;
                break;
            case TOWN: // Green
                strokeColor = 0x00FF00;
                fillColor = 0x00FF00;
                break;
            case SUBDIVISION: // Orange
                strokeColor = 0xFF9C00;
                fillColor = 0xFF9C00;
                break;
            case ADMIN: // Red
            default:
                strokeColor = 0xFF0000;
                fillColor = 0xFF0000;
                break;
        }

        /* ** Disabling this because I want to specify color directly for each type of zone.
            try {
            strokeColor = Integer.parseInt(areaStyle.strokecolor.substring(1), 16);
            fillColor = Integer.parseInt(areaStyle.fillcolor.substring(1), 16);
        } catch (NumberFormatException ignored) {
        } */

        marker.setLineStyle(areaStyle.strokeWeight, areaStyle.strokeOpacity, strokeColor);
        marker.setFillStyle(areaStyle.fillOpacity, fillColor);
        if(areaStyle.label != null) {
            marker.setLabel(areaStyle.label);
        }
    }

    /* Handle specific claim */
    private void handleClaim(@Nonnull Claim claim, Map<String, AreaMarker> newMap) {
        double[] x = null;
        double[] z = null;
        Location loc0 = claim.getLesserBoundaryCorner();
        Location loc1 = claim.getGreaterBoundaryCorner();
        if(loc0 == null)
            return;
        String worldName = claim.getWorld().getName();
        String owner = claim.isAdminClaim() ? ADMIN_ID : claim.getOwnerName().toString();
        UUID claimId = claim.getUniqueId();
        /* Handle areas */
        if(isVisible(owner, worldName, claimId.toString())) {
            /* Make outline */
            x = new double[4];
            z = new double[4];
            x[0] = loc0.getX(); z[0] = loc0.getZ();
            x[1] = loc0.getX(); z[1] = loc1.getZ() + 1.0;
            x[2] = loc1.getX() + 1.0; z[2] = loc1.getZ() + 1.0;
            x[3] = loc1.getX() + 1.0; z[3] = loc0.getZ();
            String markerId = "GP_" + claimId;
            AreaMarker marker = markersById.remove(markerId); /* Existing area? */
            if(marker == null) {
                marker = markerSet.createAreaMarker(markerId, owner, false, worldName, x, z, false);
                if(marker == null)
                    return;
            }
            else {
                marker.setCornerLocations(x, z); /* Replace corner locations */
                marker.setLabel(owner);   /* Update label */
            }
            if(config.use3d) { /* If 3D? */
                marker.setRangeY(loc1.getY()+1.0, loc0.getY());
            }
            /* Set line and fill properties */
            addStyle(owner, worldName, marker, claim);

            /* Build popup */
            String desc = formatInfoWindow(claim, marker);

            marker.setDescription(desc); /* Set popup */

            /* Add to map */
            newMap.put(markerId, marker);

            List<Claim> subclaims = claim.getChildren(false);
            for(Claim subclaim : subclaims) {
                handleClaim(subclaim, newMap);
            }
        }
    }

    /* Update grief prevention region information */
    private void updateClaims() {
        Map<String, AreaMarker> newMarkersById = new HashMap<String, AreaMarker>(); /* Build new map */
        Sponge.getServer().getWorlds().stream().map(gp::getClaimManager).map(ClaimManager::getWorldClaims).forEach(claims -> {
            claims.forEach(claim -> handleClaim(claim, newMarkersById));
        });

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldMarker : markersById.values()) {
            oldMarker.deleteMarker();
        }
        /* And replace with new map */
        markersById = newMarkersById;
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        reload();
    }

    public boolean reload() {
        logger.info("Reloading plugin.");
        return activate();
    }

    private boolean activate() {
        /* Now, get markers API */
        markerapi = dynmap.getMarkerAPI();
        if(markerapi == null) {
            logger.error("Error loading dynmap marker API.");
            return false;
        }
        /* Load configuration */
        if(reload) {
            deleteMarkerSet();
        }
        else {
            reload = true;
        }

        loadConfig();
        saveConfig();

        disablePlugin = false;

        Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(2, TimeUnit.SECONDS).submit(this);

        logger.info("Dynmap Plugin for GriefPrevention is activated.");
        return true;
    }

    private void loadConfig() {
        try {
            config = loader.load().<Config>getValue(Config.TYPE, Config::new);
        } catch (ObjectMappingException | IOException e) {
            // Disable the plugin to prevent hidden claims' locations and info from being leaked
            logger.error("Failed to load the config. Disabling plugin.", e);
            onDisable();

            // Continue loading the plugin after backing up configs
            //logger.error("Failed to load the config. Generating a default.", e);
            //config = Config.generateErrorDefault(configPath);
        }

        // Now, add marker set for mobs (make it transient)
        markerSet = markerapi.getMarkerSet("griefprevention.markerset");
        if(markerSet == null)
            markerSet = markerapi.createMarkerSet("griefprevention.markerset", config.layerName, null, false);
        else
            markerSet.setMarkerSetLabel(config.layerName);
        if(markerSet == null) {
            logger.error("Error creating marker set.");
            return;
        }

        if(config.layerMinZoom > 0)
            markerSet.setMinZoom(config.layerMinZoom);

        markerSet.setLayerPriority(config.layerPriority);
        markerSet.setHideByDefault(config.layerHideByDefault);

        // Set up update job - based on period
        if(config.updatePeriod < 15)
            config.updatePeriod = 15;
    }

    public void saveConfig() {
        try {
            loader.save(loader.createEmptyNode().setValue(Config.TYPE, config));

        } catch (ObjectMappingException | IOException e) {
            logger.warn("Error saving configuration.", e);
        }
    }

    public void onDisable() {
        deleteMarkerSet();
        disablePlugin = true;
    }

    private void deleteMarkerSet() {
        if(markerSet != null) {
            markerSet.deleteMarkerSet();
            markerSet = null;
        }
        markersById.clear();
    }

    public boolean hideClaim(Claim claim) {
        String uuid = claim.getUniqueId().toString();
        if(!config.hiddenRegions.contains(uuid)) {
            config.hiddenRegions.add(uuid);
            saveConfig();

            Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(1, TimeUnit.SECONDS).submit(this);

            return true;
        }
        else return false;
    }

    public boolean unhideClaim(Claim claim) {
        String uuid = claim.getUniqueId().toString();
        if(config.hiddenRegions.contains(uuid)) {
            config.hiddenRegions.remove(uuid);
            saveConfig();

            Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(1, TimeUnit.SECONDS).submit(this);

            return true;
        }
        else return false;
    }

    @Listener(order = Order.POST)
    public void onClaimCreate(CreateClaimEvent event) {
        Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(1, TimeUnit.SECONDS).submit(this);
    }

    @Listener(order = Order.POST)
    public void onClaimDelete(DeleteClaimEvent event) {
        Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(1, TimeUnit.SECONDS).submit(this);
    }

    @Listener(order = Order.POST)
    public void onClaimChange(ChangeClaimEvent event) {
        Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(1, TimeUnit.SECONDS).submit(this);
    }
}
