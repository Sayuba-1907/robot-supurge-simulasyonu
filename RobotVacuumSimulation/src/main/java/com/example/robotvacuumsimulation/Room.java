package com.example.robotvacuumsimulation;

public class Room {

    private final int      width;
    private final int      height;
    private final Cell[][] grid;

    public Room(int width, int height) {
        this.width  = width;
        this.height = height;
        this.grid   = new Cell[width][height];
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                grid[i][j] = new Cell();
    }

    public Cell getCell(int x, int y) {
        return isValidCoordinate(x, y) ? grid[x][y] : null;
    }

    public boolean isWalkable(int x, int y) {
        if (!isValidCoordinate(x, y)) return false;
        return !grid[x][y].isObstacle();
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public void addObstacle(int x, int y) {
        if (isValidCoordinate(x, y)) grid[x][y].setObstacle(true);
    }

    public void setChargingStation(int x, int y) {
        if (isValidCoordinate(x, y)) grid[x][y].setChargingStation(true);
    }

    public void addDirt(int x, int y, DirtType dirtType) {
        if (isValidCoordinate(x, y) && !grid[x][y].isObstacle())
            grid[x][y].setDirt(dirtType);
    }

    public void clear() {
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
            grid[x][y].setObstacle(false);
            grid[x][y].setDirt(null);
            grid[x][y].setChargingStation(false);
        }
    }
}
