package com.example.robotvacuumsimulation;

public class RoomLoader {

    public static void loadLayout(Room room, RoomType type) {
        room.clear();
        addOuterWalls(room);
        openBottomDoor(room);
        switch (type) {
            case SALON:   loadSalonContent(room);   break;
            case BEDROOM: loadBedroomContent(room); break;
            case OFFICE:  loadOfficeContent(room);  break;
        }
    }

    public static void addInitialDirt(Room room, RoomType type) {
        switch (type) {
            case SALON:
                room.addDirt(5,  5,  DirtType.DUST);
                room.addDirt(14, 4,  DirtType.LIQUID);
                room.addDirt(3,  10, DirtType.STAIN);
                break;
            case BEDROOM:
                room.addDirt(5,  6,  DirtType.DUST);
                room.addDirt(13, 6,  DirtType.LIQUID);
                room.addDirt(9,  11, DirtType.DUST);
                room.addDirt(14, 11, DirtType.STAIN);
                break;
            case OFFICE:
                room.addDirt(5,  5,  DirtType.DUST);
                room.addDirt(8,  5,  DirtType.STAIN);
                room.addDirt(13, 5,  DirtType.DUST);
                room.addDirt(16, 5,  DirtType.LIQUID);
                room.addDirt(7,  11, DirtType.DUST);
                break;
        }
    }

    private static void loadSalonContent(Room room) {
        room.setChargingStation(1, 12);
        block(room, 7,  1,  6, 2);
        block(room, 2,  4,  2, 6);
        block(room, 8,  6,  3, 3);
        block(room, 15, 6,  3, 3);
        block(room, 15, 1,  3, 1);
        block(room, 18, 9,  1, 3);
        block(room, 1,  1,  1, 1);
        block(room, 18, 1,  1, 1);
    }

    private static void loadBedroomContent(Room room) {
        room.setChargingStation(1, 12);
        block(room, 2,  2, 2, 4);
        block(room, 16, 2, 2, 4);
        block(room, 7,  2, 4, 3);
        block(room, 6,  2, 1, 1);
        block(room, 12, 2, 1, 1);
        block(room, 2,  9, 3, 2);
        block(room, 15, 9, 3, 2);
    }

    private static void loadOfficeContent(Room room) {
        room.setChargingStation(1, 12);
        block(room, 1,  1, 1, 1);
        block(room, 18, 1, 1, 1);
        block(room, 2,  2, 3, 2);
        block(room, 2,  7, 3, 2);
        block(room, 6,  2, 3, 2);
        block(room, 6,  7, 3, 2);
        block(room, 10, 1, 1, 3);
        block(room, 10, 5, 1, 5);
        block(room, 12, 2, 3, 2);
        block(room, 12, 7, 3, 2);
        block(room, 16, 2, 2, 2);
        block(room, 16, 7, 2, 2);
        block(room, 2,  10, 3, 2);
        block(room, 14, 10, 3, 2);
    }

    private static void addOuterWalls(Room room) {
        for (int x = 0; x < 20; x++) { room.addObstacle(x, 0); room.addObstacle(x, 13); }
        for (int y = 0; y < 14; y++) { room.addObstacle(0, y); room.addObstacle(19, y); }
    }

    private static void openBottomDoor(Room room) {
        room.getCell(11, 13).setObstacle(false);
        room.getCell(12, 13).setObstacle(false);
        room.getCell(13, 13).setObstacle(false);
    }

    public static void clearDirt(Room room) {
        for (int x = 0; x < 20; x++)
            for (int y = 0; y < 14; y++)
                room.getCell(x, y).setDirt(null);
    }

    public static void block(Room room, int sx, int sy, int w, int h) {
        for (int x = sx; x < sx + w; x++)
            for (int y = sy; y < sy + h; y++)
                if (x >= 0 && x < 20 && y >= 0 && y < 14)
                    room.addObstacle(x, y);
    }
}
