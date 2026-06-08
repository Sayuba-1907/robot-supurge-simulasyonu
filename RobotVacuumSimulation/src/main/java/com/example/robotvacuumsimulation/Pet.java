package com.example.robotvacuumsimulation;

import java.util.*;

public class Pet {

    private int x, y;
    private int moveCounter;
    private int moveInterval;

    private final Random random = new Random();

    private static final int[] DX = {0, 0, 1, -1};
    private static final int[] DY = {1, -1, 0, 0};

    public Pet(int startX, int startY) {
        this.x            = startX;
        this.y            = startY;
        this.moveCounter  = 0;
        this.moveInterval = 3 + random.nextInt(3);
    }

    public void tick(Room room, int robotX, int robotY) {
        moveCounter++;
        if (moveCounter >= moveInterval) {
            moveCounter  = 0;
            moveInterval = 3 + random.nextInt(3);
            tryMove(room, robotX, robotY);
        }
    }

    private void tryMove(Room room, int robotX, int robotY) {
        List<Integer> dirs = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(dirs, random);
        for (int i : dirs) {
            int nx = x + DX[i];
            int ny = y + DY[i];
            if (nx >= 1 && nx < 19 && ny >= 1 && ny < 13
                    && room.isWalkable(nx, ny)
                    && !(nx == robotX && ny == robotY)) {
                x = nx;
                y = ny;
                return;
            }
        }
    }

    public void reset(int startX, int startY) {
        x            = startX;
        y            = startY;
        moveCounter  = 0;
        moveInterval = 3 + random.nextInt(3);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
