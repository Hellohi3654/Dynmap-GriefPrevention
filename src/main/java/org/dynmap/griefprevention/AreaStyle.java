package org.dynmap.griefprevention;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class AreaStyle {
    @Setting("strokeColor")
    String strokecolor = "#FF0000";
    @Setting("strokeOpacity")
    double strokeopacity = 0.8d;
    @Setting("strokeWeight")
    int strokeweight = 3;
    @Setting("fillColor")
    String fillcolor = "#FF0000";
    @Setting("fillOpacity")
    double fillopacity = 0.35d;
    @Setting("label")
    String label = "";
}
