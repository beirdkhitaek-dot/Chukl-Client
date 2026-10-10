package com.chukl.client.modules.donut;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.core.Render3D;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ColorSetting;
import com.chukl.client.settings.ModeSetting;
import com.chukl.client.settings.NumberSetting;
import com.chukl.client.suschunk.SusChunkRenderer;
import com.chukl.client.suschunk.SusChunkScanner;

/** 67Client's Sus Chunk Finder: finds long-loaded chunks (bases) via amethyst server light and plant growth. */
public class SixSevenSusChunkFinder extends Module {
    public final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", "Higher = stricter: more weighted evidence before a chunk flags (x5 points)", 3, 1, 10, 1));
    public final BoolSetting amethyst = add(new BoolSetting("Amethyst", "Hidden grown clusters via server block light 5, the strongest signal", true));
    public final BoolSetting kelp = add(new BoolSetting("Kelp", "Fully grown / unusually tall kelp columns", true));
    public final BoolSetting bamboo = add(new BoolSetting("Bamboo", "Fully grown, max-height bamboo", true));
    public final BoolSetting berries = add(new BoolSetting("Berries", "Sweet berry bushes at max growth stage", true));
    public final BoolSetting vines = add(new BoolSetting("Vines", "Vines grown far down from their support", true));
    public final BoolSetting dripstone = add(new BoolSetting("Dripstone", "Dripstone spikes longer than natural generation", true));
    public final NumberSetting scanSpeed = add(new NumberSetting("Scan Speed", "Chunks scanned per tick", 10, 2, 24, 1));
    public final NumberSetting renderY = add(new NumberSetting("Render Y", "Height the flat chunk highlights render at", 100, -64, 320, 1));
    public final ColorSetting color = add(new ColorSetting("Color", "Color of the chunk highlights", 0xFFB066FF));
    public final NumberSetting fillOpacity = add(new NumberSetting("Fill Opacity", "Fill opacity of the chunk quads", 90, 0, 255, 1));
    public final BoolSetting outline = add(new BoolSetting("Outline", "Crisp border along the chunk edges", true));
    public final NumberSetting outlineOpacity = add(new NumberSetting("Outline Opacity", "Border opacity", 200, 0, 255, 1));
    public final BoolSetting smartMode = add(new BoolSetting("Smart Mode", "Merge nearby flags into zones with a centroid marker", true));
    public final NumberSetting mergeRadius = add(new NumberSetting("Merge Radius", "Flags within this many chunks merge into one zone", 3, 1, 8, 1));
    public final BoolSetting centroidMarker = add(new BoolSetting("Centroid Marker", "Mark each zone's weighted centre, the likely base spot", true));
    public final ModeSetting notifications = add(new ModeSetting("Notifications", "Announce each new zone once, with coords and distance", "Chat", "Chat", "Off"));
    public final SusChunkScanner scanner = new SusChunkScanner(this);

    public SixSevenSusChunkFinder() {
        super(Category.DONUT, "67 Sus Chunk Finder", "Finds long-loaded chunks (bases) via amethyst light and growth (from 67Client)");
        outlineOpacity.visibleWhen(outline::get);
    }

    @Override
    protected void onTick() {
        scanner.tick();
    }

    @Override
    protected void onDisable() {
        scanner.clear();
        SusChunkRenderer.reset();
    }

    @Override
    protected void onRender3D(Render3D r) {
        SusChunkRenderer.render(r, this);
    }
}
