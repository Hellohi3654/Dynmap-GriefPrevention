package org.dynmap.griefprevention;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.GriefPreventionApi;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.event.ChangeClaimEvent;
import me.ryanhamshire.griefprevention.api.event.CreateClaimEvent;
import me.ryanhamshire.griefprevention.api.event.DeleteClaimEvent;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Plugin(
        id = "dynmapgriefprevention",
        name = "Dynmap",
        dependencies = { @Dependency(id = "dynmap"), @Dependency(id = "griefprevention")}
)
public class DynmapGriefprevention {

    @Inject private Logger logger;

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
    private boolean reload;

    private DynmapCommonAPI dynmap;
    private MarkerAPI markerapi;
    private GriefPreventionApi gp;

    @Inject()
    @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;

    private ConfigurationNode cfg;

    private MarkerSet set;
    private long updperiod;
    private boolean use3d;
    private String infowindow;
    private String admininfowindow;
    private AreaStyle defstyle = new AreaStyle();
    private Map<String, AreaStyle> ownerstyle;
    private Set<String> visible;
    private Set<String> hidden;
    private boolean stop;
    private int maxdepth;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Initializing");

        gp = GriefPrevention.getApi();

        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI api) {
                dynmap = api;
                markerapi = api.getMarkerAPI();
                try {
                    cfg = loader.load();
                    activate();
                } catch (ObjectMappingException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private Map<String, AreaMarker> resareas = new HashMap<String, AreaMarker>();

    private class GriefPreventionUpdate implements Consumer<Task> {

        @Override
        public void accept(Task task) {
            if(!stop) {
                updateClaims();
            } else {
                task.cancel();
            }
        }
    }

    private String formatInfoWindow(Claim claim, AreaMarker m) {
        String v;
        if(claim.isAdminClaim())
            v = "<div class=\"regioninfo\">"+admininfowindow+"</div>";
        else
            v = "<div class=\"regioninfo\">"+infowindow+"</div>";
        v = v.replace("%owner%", claim.isAdminClaim() ? ADMIN_ID : claim.getOwnerName().toPlain());
        v = v.replace("%area%", Integer.toString(claim.getArea()));
        v = v.replace("%claimname%", claim.getData().getName().isPresent() ? claim.getName().get().toPlain() : "No Name Set");
        v = v.replace("%lastseen%", claim.getData().getDateLastActive().toString());
        v = v.replace("%gptype%", claim.getType().toString());

        //claim.getData().getDateLastActive()
        ArrayList<String> builders = new ArrayList<String>();
        ArrayList<String> containers = new ArrayList<String>();
        ArrayList<String> accessors = new ArrayList<String>();
        ArrayList<String> managers = new ArrayList<String>();
        //claim.getPermissions()builders, containers, accessors, managers);

        /* Build builders list */
        String accum = "";
        for(int i = 0; i < builders.size(); i++) {
            if(i > 0) accum += ", ";
            accum += builders.get(i);
        }
        v = v.replace("%builders%", accum);
        /* Build containers list */
        accum = "";
        for(int i = 0; i < containers.size(); i++) {
            if(i > 0) accum += ", ";
            accum += containers.get(i);
        }
        v = v.replace("%containers%", accum);
        /* Build accessors list */
        accum = "";
        for(int i = 0; i < accessors.size(); i++) {
            if(i > 0) accum += ", ";
            accum += accessors.get(i);
        }
        v = v.replace("%accessors%", accum);
        /* Build managers list */
        accum = "";
        for(int i = 0; i < managers.size(); i++) {
            if(i > 0) accum += ", ";
            accum += managers.get(i);
        }
        v = v.replace("%managers%", accum);

        return v;
    }

    private boolean isVisible(String owner, String worldname) {
        if((visible != null) && (visible.size() > 0)) {
            if((!visible.contains(owner)) && (!visible.contains("world:" + worldname)) &&
                    (!visible.contains(worldname + "/" + owner))) {
                return false;
            }
        }
        if((hidden != null) && (hidden.size() > 0)) {
            if(hidden.contains(owner) || hidden.contains("world:" + worldname) || hidden.contains(worldname + "/" + owner))
                return false;
        }
        return true;
    }

    private void addStyle(String owner, String worldid, AreaMarker m, Claim claim) {
        AreaStyle as = null;

        if(!ownerstyle.isEmpty()) {
            as = ownerstyle.get(owner.toLowerCase());
        }

        if(as == null)
            as = defstyle;

        int sc = 0xFF0000;
        int fc = 0xFF0000;


        if (claim.getType().equals(ClaimType.ADMIN)) { // Red
            sc = 0xFF0000;
            fc = 0xFF0000;
        } else if (claim.getType().equals(ClaimType.BASIC)) { // Yellow
            sc = 0xFFFF00;
            fc = 0xFFFF00;
        } else  if (claim.getType().equals(ClaimType.TOWN)) { // Green
            sc = 0x00FF00;
            fc = 0x00FF00;
        } else  if (claim.getType().equals(ClaimType.SUBDIVISION)) { // Orange
            sc = 0xFF9C00;
            fc = 0xFF9C00;
        }

        /* ** Disabling this because I want to specify color directly for each type of zone.
            try {
            sc = Integer.parseInt(as.strokecolor.substring(1), 16);
            fc = Integer.parseInt(as.fillcolor.substring(1), 16);
        } catch (NumberFormatException ignored) {
        } */

        m.setLineStyle(as.strokeweight, as.strokeopacity, sc);
        m.setFillStyle(as.fillopacity, fc);
        if(as.label != null) {
            m.setLabel(as.label);
        }
    }

    /* Handle specific claim */
    private void handleClaim(Claim claim, Map<String, AreaMarker> newmap) {
        double[] x = null;
        double[] z = null;
        Location l0 = claim.getLesserBoundaryCorner();
        Location l1 = claim.getGreaterBoundaryCorner();
        if(l0 == null)
            return;
        String wname = claim.getWorld().getName();
        String owner = claim.isAdminClaim() ? ADMIN_ID : claim.getOwnerName().toString();
        /* Handle areas */
        if(isVisible(owner, wname)) {
            /* Make outline */
            x = new double[4];
            z = new double[4];
            x[0] = l0.getX(); z[0] = l0.getZ();
            x[1] = l0.getX(); z[1] = l1.getZ() + 1.0;
            x[2] = l1.getX() + 1.0; z[2] = l1.getZ() + 1.0;
            x[3] = l1.getX() + 1.0; z[3] = l0.getZ();
            UUID id = claim.getUniqueId();
            String markerid = "GP_" + id;
            AreaMarker m = resareas.remove(markerid); /* Existing area? */
            if(m == null) {
                m = set.createAreaMarker(markerid, owner, false, wname, x, z, false);
                if(m == null)
                    return;
            }
            else {
                m.setCornerLocations(x, z); /* Replace corner locations */
                m.setLabel(owner);   /* Update label */
            }
            if(use3d) { /* If 3D? */
                m.setRangeY(l1.getY()+1.0, l0.getY());
            }
            /* Set line and fill properties */
            addStyle(owner, wname, m, claim);

            /* Build popup */
            String desc = formatInfoWindow(claim, m);

            m.setDescription(desc); /* Set popup */

            /* Add to map */
            newmap.put(markerid, m);
            
            List<Claim> subclaims = claim.getChildren(false);
            for(Claim subclaim : subclaims) {
                handleClaim(subclaim, newMap);
            }
        }
    }

    /* Update grief prevention region information */
    @SuppressWarnings("unchecked")
    private void updateClaims() {
        Map<String,AreaMarker> newmap = new HashMap<String,AreaMarker>(); /* Build new map */

        //final ArrayList<Claim> claims = new ArrayList<>();

        Sponge.getServer().getWorlds().stream().map(gp::getClaimManager).map(ClaimManager::getWorldClaims).forEach(claims -> {
            claims.forEach(claim -> handleClaim(claim, newmap));
        });

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldm : resareas.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        resareas = newmap;
    }

    private void activate() throws ObjectMappingException {
        /* Now, get markers API */
        markerapi = dynmap.getMarkerAPI();
        if(markerapi == null) {
            logger.error("Error loading dynmap marker API!");
            return;
        }
        /* Load configuration */
        if(reload) {
            //reloadConfig();
            if(set != null) {
                set.deleteMarkerSet();
                set = null;
            }
            resareas.clear();
        }
        else {
            reload = true;
        }
        //FileConfiguration cfg = getConfig();
        //cfg.options().copyDefaults(true);   /* Load defaults, if needed */
        //this.saveConfig();  /* Save updates, if needed */

        /* Now, add marker set for mobs (make it transient) */
        set = markerapi.getMarkerSet("griefprevention.markerset");
        if(set == null)
            set = markerapi.createMarkerSet("griefprevention.markerset", cfg.getNode("layer", "name").getString("GriefPrevention"), null, false);
        else
            set.setMarkerSetLabel(cfg.getNode("layer", "name").getString("GriefPrevention"));
        if(set == null) {
            logger.error("Error creating marker set");
            return;
        }

        int minzoom = cfg.getNode("layer", "minzoom").getInt(0);
        if(minzoom > 0)
            set.setMinZoom(minzoom);

        set.setLayerPriority(cfg.getNode("layer", "layerprio").getInt(10));
        set.setHideByDefault(cfg.getNode("layer", "hidebydefault").getBoolean(false));
        use3d = cfg.getNode("use3dregions").getBoolean(false);
        infowindow = cfg.getNode("infowindow").getString(DEF_INFOWINDOW);
        admininfowindow = cfg.getNode("adminclaiminfowindow").getString(DEF_ADMININFOWINDOW);
        maxdepth = cfg.getNode("maxdepth").getInt(16);

        /* Get style information */
        defstyle = cfg.getNode("regionstyle").getValue(TypeToken.of(AreaStyle.class), new AreaStyle());
        ownerstyle = new HashMap<String, AreaStyle>();

        ConfigurationNode sect = cfg.getNode("ownerstyle");
        if(!sect.isVirtual()) {
            Map<String, AreaStyle> map = sect.getValue(new TypeToken<Map<String, AreaStyle>>() {});

            ownerstyle.putAll(map);
        }

        List<String> vis = cfg.getNode("visibleregions").getList(TypeToken.of(String.class));
        if(vis != null) {
            visible = new HashSet<String>(vis);
        }
        List<String> hid = cfg.getNode("hiddenregions").getList(TypeToken.of(String.class));
        if(hid != null) {
            hidden = new HashSet<String>(hid);
        }

        /* Set up update job - based on periond */
        int per = cfg.getNode("update", "period").getInt(300);
        if(per < 15) per = 15;
        stop = false;

        Sponge.getScheduler().createTaskBuilder().execute(new GriefPreventionUpdate()).delay(2, TimeUnit.SECONDS).submit(this);

        logger.info("Dynmap Plugin for GriefPrevention  is activated");
    }


    public void onDisable() {
        if(set != null) {
            set.deleteMarkerSet();
            set = null;
        }
        resareas.clear();
        stop = true;
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
