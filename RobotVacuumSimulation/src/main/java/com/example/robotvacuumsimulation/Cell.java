package com.example.robotvacuumsimulation;

public class Cell {

    private boolean isObstacle;
    private boolean isChargingStation;
    private DirtType dirt;

    public Cell() {
        this.isObstacle        = false;
        this.isChargingStation = false;
        this.dirt              = null;
    }

    public boolean isObstacle()                    { return isObstacle; }
    public void    setObstacle(boolean obstacle)   { this.isObstacle = obstacle; }

    public boolean isChargingStation()                          { return isChargingStation; }
    public void    setChargingStation(boolean chargingStation)  { this.isChargingStation = chargingStation; }

    public DirtType getDirt()               { return dirt; }
    public void     setDirt(DirtType dirt)  { this.dirt = dirt; }

    public boolean hasDirt() { return this.dirt != null; }
}
