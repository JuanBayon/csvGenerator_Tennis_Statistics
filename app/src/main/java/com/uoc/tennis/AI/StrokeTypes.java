package com.uoc.tennis.AI;

import com.uoc.tennis.MainActivity;

public enum StrokeTypes {
    DRIVE(MainActivity.DRIVE),
    BACKHAND(MainActivity.BACKHAND),
    SERVICE(MainActivity.SERVICE),
    SMASH(MainActivity.SMASH),
    VR(MainActivity.VR),
    VL(MainActivity.VL);

    private String smash;

    StrokeTypes(String smash) {
        this.smash = smash;
    }

    public String getSmash() {
        return smash;
    }

    public void setSmash(String smash) {
        this.smash = smash;
    }

    public StrokeTypes getStrokeType(String smash) {
        for (StrokeTypes stroke: StrokeTypes.values()) {
            if (stroke.name().equals(smash)) {
                return stroke;
            }
        }
        return null;
    }
}
