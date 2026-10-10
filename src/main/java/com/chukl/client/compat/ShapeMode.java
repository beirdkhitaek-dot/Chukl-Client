package com.chukl.client.compat;

import com.chukl.client.core.Render3D;

public enum ShapeMode {
    Lines, Sides, Both;

    public static ShapeMode get(boolean lines, boolean sides) {
        if (lines && sides) return Both;
        return lines ? Lines : Sides;
    }

    public boolean lines() { return this != Sides; }
    public boolean sides() { return this != Lines; }

    public Render3D.Mode mode() {
        return this == Lines ? Render3D.Mode.LINES : this == Sides ? Render3D.Mode.SIDES : Render3D.Mode.BOTH;
    }
}
