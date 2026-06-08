package com.example.robotvacuumsimulation;

public enum DirtType {

    DUST(1.0, 1),
    LIQUID(3.0, 3),
    STAIN(5.0, 5);

    private final double cleaningTime;
    private final int    batteryCost;

    DirtType(double cleaningTime, int batteryCost) {
        this.cleaningTime = cleaningTime;
        this.batteryCost  = batteryCost;
    }

    public double getCleaningTime() { return cleaningTime; }
    public int getBatteryCost() { return batteryCost; }
}
