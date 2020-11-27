package org.dynmap.griefprevention;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class AreaStyle {

    @Setting("strokeColor")
    String strokeColor = "#FF0000";

    @Setting("strokeOpacity")
    double strokeOpacity = 0.8d;

    @Setting("strokeWeight")
    int strokeWeight = 3;

    @Setting("fillColor")
    String fillColor = "#FF0000";

    @Setting("fillOpacity")
    double fillOpacity = 0.35d;

    @Setting("label")
    String label = "No label";
}
