package com.example.robotvacuumsimulation;

public class Robot {

    private int       x;
    private int       y;
    private double    battery;
    private Direction direction;

    private static final double MOVE_COST = 0.5;

    public Robot(int startX, int startY) {
        this.x         = startX;
        this.y         = startY;
        this.battery   = 100.0;
        this.direction = Direction.EAST;
    }

    public void move(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        this.battery -= MOVE_COST;
        if (this.battery < 0) this.battery = 0;
    }

    public void cleanDirt(DirtType dirt) {
        this.battery -= dirt.getBatteryCost();
        if (this.battery < 0) this.battery = 0;
    }

    public void recharge() { this.battery = 100.0; }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public double getBattery()                   { return battery; }
    public void   setBattery(double battery)     { this.battery = battery; }

    public Direction getDirection()                       { return direction; }
    public void      setDirection(Direction direction)    { this.direction = direction; }
}
