package com.example.robotvacuumsimulation;

import javafx.animation.AnimationTimer;
import javafx.scene.input.MouseEvent;
import java.util.*;

public class SimulationController {

    private enum MultiRoomPhase {
        IN_SALON,
        HEADING_TO_DOOR,
        TRANSITION_OUT,
        IN_TARGET_ROOM,
        HEADING_TO_EXIT,
        TRANSITION_IN,
        RETURNING_HOME,
        DONE
    }

    private static final int TRANSITION_HALFWAY = 3;
    private static final int TRANSITION_TOTAL   = 6;

    private static final int DOOR_X = 12;
    private static final int DOOR_Y = 12;

    private Robot          robot;
    private Room           salonRoom;
    private Room           targetRoomObj;
    private Room           room;
    private SimulationView view;

    private RoomType       targetRoomType;
    private MultiRoomPhase phase = MultiRoomPhase.IN_SALON;
    private int            transitionTimer         = 0;
    private boolean        targetRoomFullyCleaned  = false;

    private boolean       isRunning   = false;
    private AnimationTimer timer;
    private long          lastUpdate  = 0;
    private int           currentTool = 0;

    private long startTime;
    private long elapsedTimeInSeconds  = 0;
    private int  totalCleanableArea    = (20 * 14);
    private int  cleanedCells          = 0;
    private int  unreachableAreaCount  = 0;
    private int  dirtyCount            = 0;

    private Queue<Point> returnPath = new LinkedList<>();
    private Queue<Point> dirtPath   = new LinkedList<>();
    private Queue<Point> doorPath   = new LinkedList<>();

    private boolean isReturningToStation = false;
    private boolean isSeekingDirt        = false;
    private int     cleaningDelayTimer   = 0;

    private int spiralStepsTarget = 1;
    private int spiralStepsTaken  = 0;
    private int spiralTurns       = 0;

    private boolean[][] wfVisited       = new boolean[20][14];
    private int         wfSameAreaSteps = 0;
    private static final int WF_ESCAPE_THRESHOLD = 12;
    private Queue<Point>     wfEscapePath = new LinkedList<>();
    private boolean          wfEscaping   = false;

    private SoundManager soundManager;
    private Pet          cat;

    private static class PlacedObstacle {
        final int x, y, w, h;
        final String type;
        final List<Point> owned;

        PlacedObstacle(int x, int y, int w, int h, String type, List<Point> owned) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.type = type; this.owned = owned;
        }
    }
    private List<PlacedObstacle> placedObstacles = new ArrayList<>();

    public SimulationController(Robot robot, Room salonRoom, SimulationView view, RoomType targetRoomType) {
        this.robot          = robot;
        this.salonRoom      = salonRoom;
        this.room           = salonRoom;
        this.view           = view;
        this.targetRoomType = targetRoomType;

        soundManager = new SoundManager();
        cat = new Pet(6, 3);
        view.setCat(cat);

        RoomLoader.loadLayout(salonRoom, RoomType.SALON);

        if (targetRoomType != RoomType.SALON) {
            targetRoomObj = new Room(20, 14);
            RoomLoader.loadLayout(targetRoomObj, targetRoomType);
            RoomLoader.addInitialDirt(targetRoomObj, targetRoomType);
            phase     = MultiRoomPhase.HEADING_TO_DOOR;
            dirtyCount = countDirty(targetRoomObj);
        } else {
            RoomLoader.addInitialDirt(salonRoom, RoomType.SALON);
            phase     = MultiRoomPhase.IN_SALON;
            dirtyCount = countDirty(salonRoom);
        }

        view.setCurrentRoom(RoomType.SALON);
        view.setOnRoomNavigate(this::navigateToRoom);
        detectUnreachableAreas();
        setupEventHandlers();
        setupTimer();

        if (phase == MultiRoomPhase.HEADING_TO_DOOR) computeDoorPath();
        else seekNextDirt();

        view.render(room, robot);
    }

    private void setupEventHandlers() {
        view.getBtnStart().setOnAction(e -> startSimulation());
        view.getBtnPause().setOnAction(e -> pauseSimulation());
        view.getBtnReset().setOnAction(e -> resetSimulation());

        view.getBtnReturn().setOnAction(e -> {
            if (targetRoomType != RoomType.SALON && room != salonRoom) {
                targetRoomFullyCleaned = false;
                phase = MultiRoomPhase.HEADING_TO_EXIT;
                isSeekingDirt = false;
                dirtPath.clear();
                computeDoorPath();
                startSimulation();
            } else {
                returnToStation();
            }
        });

        view.getSliderBattery().valueProperty().addListener((obs, oldVal, newVal) -> {
            robot.setBattery(newVal.doubleValue());
            view.render(room, robot);
        });

        view.getCanvas().widthProperty().addListener(e -> view.render(room, robot));
        view.getCanvas().heightProperty().addListener(e -> view.render(room, robot));

        view.getBtnAddDirt().setOnAction(e -> currentTool = 1);
        view.getBtnAddObstacle().setOnAction(e -> currentTool = 2);
        view.getBtnRemoveFurniture().setOnAction(e -> currentTool = 3);

        view.getCanvas().setOnMouseClicked(this::handleMouseClick);

        view.getRbSpiral().setOnAction(e -> { spiralStepsTarget = 1; spiralStepsTaken = 0; spiralTurns = 0; });
    }

    private void returnToStation() {
        isReturningToStation = true;
        isSeekingDirt = false;
        dirtPath.clear();
        view.setReturningStatus(true);

        List<Point> path = findPathBFS(robot.getX(), robot.getY(), 1, 12);
        returnPath.clear();
        if (!path.isEmpty()) {
            returnPath.addAll(path);
        } else {
            Cell c = room.getCell(robot.getX(), robot.getY());
            if (c == null || !c.isChargingStation()) {
                isReturningToStation = false;
                view.setReturningStatus(false);
            }
        }
        if (isReturningToStation) startSimulation();
    }

    private void handleMouseClick(MouseEvent event) {
        double[] m = view.getGridOffsetAndSize();
        double oX = m[0], oY = m[1], cs = m[2];
        int gridX = (int) Math.floor((event.getX() - oX) / cs);
        int gridY = (int) Math.floor((event.getY() - oY) / cs);

        if (gridX < 0 || gridX >= 20 || gridY < 0 || gridY >= 14) return;

        if (currentTool == 1) {
            DirtType sel = DirtType.DUST;
            if (view.getRbLiquid().isSelected()) sel = DirtType.LIQUID;
            if (view.getRbStain().isSelected())  sel = DirtType.STAIN;
            Cell c      = room.getCell(gridX, gridY);
            boolean was = c != null && c.hasDirt();
            room.addDirt(gridX, gridY, sel);
            if (!was && c != null && c.hasDirt()) dirtyCount++;
            if (!isReturningToStation && phase != MultiRoomPhase.HEADING_TO_DOOR && phase != MultiRoomPhase.TRANSITION_OUT) {
                seekNextDirt();
            }
        } else if (currentTool == 2) {
            int w = 1, h = 1; String type = "WALL";
            if (view.getRbSofa()  != null && view.getRbSofa().isSelected())  { w = 6; h = 2; type = "SOFA"; }
            else if (view.getRbTable() != null && view.getRbTable().isSelected()) { w = 3; h = 3; type = "TABLE"; }
            if (!isValidPlacement(gridX, gridY, w, h)) return;
            List<Point> owned = placeObstacleBlock(gridX, gridY, w, h);
            if (!owned.isEmpty()) {
                placedObstacles.add(new PlacedObstacle(gridX, gridY, w, h, type, owned));
                view.addUserObject(gridX, gridY, w, h, type);
                detectUnreachableAreas();
            }
        } else if (currentTool == 3) {
            removeFurnitureAt(gridX, gridY);
        }
        view.render(room, robot);
    }

    private boolean isValidPlacement(int sx, int sy, int w, int h) {
        for (int x = sx; x < sx + w; x++) for (int y = sy; y < sy + h; y++) {
            if (x <= 0 || x >= 19 || y <= 0 || y >= 13) return false;
            Cell c = room.getCell(x, y);
            if (c != null && c.isChargingStation()) return false;
        }
        return true;
    }

    private List<Point> placeObstacleBlock(int sx, int sy, int w, int h) {
        List<Point> owned = new ArrayList<>();
        for (int x = sx; x < sx + w; x++) for (int y = sy; y < sy + h; y++) {
            if (x > 0 && x < 19 && y > 0 && y < 13) {
                Cell c = room.getCell(x, y);
                if (c != null && !c.isObstacle()) { room.addObstacle(x, y); owned.add(new Point(x, y)); }
            }
        }
        return owned;
    }

    private void removeFurnitureAt(int gx, int gy) {
        for (int i = placedObstacles.size() - 1; i >= 0; i--) {
            PlacedObstacle po = placedObstacles.get(i);
            if (gx >= po.x && gx < po.x + po.w && gy >= po.y && gy < po.y + po.h) {
                for (Point p : po.owned) {
                    Cell c = room.getCell(p.x, p.y);
                    if (c != null) c.setObstacle(false);
                }
                placedObstacles.remove(i);
                view.removeUserObjectAt(gx, gy);
                detectUnreachableAreas();
                break;
            }
        }
    }

    private int countDirty(Room r) {
        int count = 0;
        for (int x = 0; x < 20; x++) for (int y = 0; y < 14; y++) {
            Cell c = r.getCell(x, y);
            if (c != null && c.hasDirt()) count++;
        }
        return count;
    }

    private void setupTimer() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double spd      = view.getSliderSpeed().getValue();
                long   interval = (long)(500_000_000 / spd);
                if (now - lastUpdate >= interval) {
                    if (isRunning) {
                        updateSimulation();
                        updateStatistics();
                        view.render(room, robot);
                    }
                    lastUpdate = now;
                }
            }
        };
    }

    public void startSimulation() {
        if (!isRunning) {
            startTime = System.currentTimeMillis() - (elapsedTimeInSeconds * 1000);
            isRunning = true;
            soundManager.startVacuum();
            timer.start();
        }
    }

    public void pauseSimulation() {
        isRunning = false;
        soundManager.stopVacuum();
    }

    public void resetSimulation() {
        isRunning = false;
        soundManager.stopAll();

        cat.reset(6, 3);
        robot = new Robot(11, 7);

        cleanedCells = 0;
        elapsedTimeInSeconds = 0;

        isReturningToStation = false;
        isSeekingDirt = false;
        targetRoomFullyCleaned = false;

        view.setReturningStatus(false);
        view.hideTransition();
        view.stopCleaningAnimation();

        returnPath.clear();
        dirtPath.clear();
        doorPath.clear();
        cleaningDelayTimer = 0;

        spiralStepsTarget = 1;
        spiralStepsTaken  = 0;
        spiralTurns       = 0;

        wfVisited       = new boolean[20][14];
        wfSameAreaSteps = 0;
        wfEscaping      = false;
        wfEscapePath.clear();

        view.resetPathHistory();
        view.clearUserObjects();
        view.setCurrentRoom(RoomType.SALON);
        placedObstacles.clear();

        RoomLoader.loadLayout(salonRoom, RoomType.SALON);
        room = salonRoom;

        if (targetRoomType != RoomType.SALON && targetRoomObj != null) {
            RoomLoader.loadLayout(targetRoomObj, targetRoomType);
            RoomLoader.addInitialDirt(targetRoomObj, targetRoomType);
            phase      = MultiRoomPhase.HEADING_TO_DOOR;
            dirtyCount = countDirty(targetRoomObj);
            computeDoorPath();
        } else {
            targetRoomType = RoomType.SALON;
            targetRoomObj  = null;
            RoomLoader.addInitialDirt(salonRoom, RoomType.SALON);
            phase      = MultiRoomPhase.IN_SALON;
            dirtyCount = countDirty(salonRoom);
            seekNextDirt();
        }

        detectUnreachableAreas();
        view.updateStats(0, totalCleanableArea, unreachableAreaCount, dirtyCount, "00:00");
        view.render(room, robot);
    }

    private void updateSimulation() {
        if (room == salonRoom) cat.tick(room, robot.getX(), robot.getY());

        if (robot.getBattery() <= 0) { pauseSimulation(); return; }

        if (cleaningDelayTimer > 0) { cleaningDelayTimer--; return; }

        if (phase != MultiRoomPhase.IN_SALON && phase != MultiRoomPhase.DONE && handleMultiRoomPhase()) return;

        if (isReturningToStation) {
            if (!returnPath.isEmpty()) {
                moveToPoint(returnPath.poll());
                view.getSliderBattery().setValue(robot.getBattery());
            } else {
                Cell stCell   = room.getCell(robot.getX(), robot.getY());
                boolean atSt  = stCell != null && stCell.isChargingStation();
                if (atSt && robot.getBattery() < 100.0) {
                    soundManager.stopVacuum();
                    soundManager.startCharging();
                    double nb = Math.min(100.0, robot.getBattery() + 5.0);
                    robot.setBattery(nb);
                    view.getSliderBattery().setValue(nb);
                } else if (atSt) {
                    soundManager.stopCharging();
                    isReturningToStation = false;
                    view.setReturningStatus(false);
                    dirtPath.clear();
                    isSeekingDirt = false;
                    if (targetRoomType != RoomType.SALON && !targetRoomFullyCleaned) {
                        phase = MultiRoomPhase.HEADING_TO_DOOR;
                        computeDoorPath();
                        startSimulation();
                    } else {
                        phase = MultiRoomPhase.IN_SALON;
                        targetRoomType = RoomType.SALON;
                        seekNextDirt();
                        if (!isSeekingDirt) pauseSimulation();
                    }
                } else {
                    isReturningToStation = false;
                    view.setReturningStatus(false);
                }
            }
            return;
        }

        if (robot.getBattery() <= 20.0) {
            if (room != salonRoom) {
                targetRoomFullyCleaned = false;
                phase = MultiRoomPhase.HEADING_TO_EXIT;
                dirtPath.clear();
                isSeekingDirt = false;
                computeDoorPath();
                view.setReturningStatus(true);
            } else {
                returnToStation();
            }
            return;
        }

        Cell currentCell = room.getCell(robot.getX(), robot.getY());
        if (currentCell != null && currentCell.hasDirt()) {
            DirtType dirt = currentCell.getDirt();
            robot.cleanDirt(dirt);
            currentCell.setDirt(null);
            cleanedCells++;
            dirtyCount = Math.max(0, dirtyCount - 1);
            view.getSliderBattery().setValue(robot.getBattery());

            int animFrames = Math.max(2, (int)(dirt.getCleaningTime() * 2));
            cleaningDelayTimer = animFrames;
            soundManager.playCleaning();
            view.startCleaningAnimation(robot.getX(), robot.getY(), dirt, animFrames);

            dirtPath.clear();
            isSeekingDirt = false;
            seekNextDirt();
            if (room != salonRoom && !isSeekingDirt && findNearestDirt() == null) {
                targetRoomFullyCleaned = true;
                phase = MultiRoomPhase.HEADING_TO_EXIT;
                computeDoorPath();
            }
            return;
        }

        if (isSeekingDirt && !dirtPath.isEmpty()) {
            Point next = dirtPath.peek();
            if (!isWalkableForRobot(next.x, next.y)) {
                dirtPath.clear();
                isSeekingDirt = false;
                seekNextDirt();
                return;
            }
            dirtPath.poll();
            moveToPoint(next);
            view.getSliderBattery().setValue(robot.getBattery());
            if (dirtPath.isEmpty()) isSeekingDirt = false;
            return;
        }

        if      (view.getRbSpiral().isSelected())     moveSpiral();
        else if (view.getRbWallFollow().isSelected()) moveWallFollowing();
        else                                           moveRandomly();
        view.getSliderBattery().setValue(robot.getBattery());

        if (room != salonRoom && phase == MultiRoomPhase.IN_TARGET_ROOM && !isSeekingDirt && findNearestDirt() == null) {
            targetRoomFullyCleaned = true;
            phase = MultiRoomPhase.HEADING_TO_EXIT;
            computeDoorPath();
        }
    }

    private boolean handleMultiRoomPhase() {
        switch (phase) {

            case HEADING_TO_DOOR: {
                if (doorPath.isEmpty()) {
                    transitionTimer = 0;
                    phase = MultiRoomPhase.TRANSITION_OUT;
                    view.showTransition("-> " + targetRoomType.displayName);
                    return true;
                }
                moveToPoint(doorPath.poll());
                view.getSliderBattery().setValue(robot.getBattery());
                return true;
            }

            case TRANSITION_OUT: {
                transitionTimer++;
                if (transitionTimer == TRANSITION_HALFWAY) {
                    room = targetRoomObj;
                    robot.setPosition(DOOR_X, DOOR_Y - 1);
                    view.setCurrentRoom(targetRoomType);
                    view.resetPathHistory();
                    wfVisited       = new boolean[20][14];
                    wfSameAreaSteps = 0;
                    wfEscaping      = false;
                    wfEscapePath.clear();
                    cleanedCells = 0;
                    dirtyCount   = countDirty(room);
                    detectUnreachableAreas();
                    seekNextDirt();
                }
                if (transitionTimer >= TRANSITION_TOTAL) {
                    phase = MultiRoomPhase.IN_TARGET_ROOM;
                    view.hideTransition();
                }
                return true;
            }

            case IN_TARGET_ROOM:
                return false;

            case HEADING_TO_EXIT: {
                if (doorPath.isEmpty()) {
                    transitionTimer = 0;
                    phase = MultiRoomPhase.TRANSITION_IN;
                    view.showTransition("<- Salon'a donuluyor...");
                    return true;
                }
                moveToPoint(doorPath.poll());
                view.getSliderBattery().setValue(robot.getBattery());
                return true;
            }

            case TRANSITION_IN: {
                transitionTimer++;
                if (transitionTimer == TRANSITION_HALFWAY) {
                    room = salonRoom;
                    robot.setPosition(DOOR_X, DOOR_Y - 1);
                    view.setCurrentRoom(RoomType.SALON);
                    view.setReturningStatus(false);
                    view.resetPathHistory();
                    dirtyCount = countDirty(room);
                    detectUnreachableAreas();
                    List<Point> path = findPathBFS(DOOR_X, DOOR_Y - 1, 1, 12);
                    returnPath.clear();
                    returnPath.addAll(path);
                    isReturningToStation = true;
                    view.setReturningStatus(true);
                }
                if (transitionTimer >= TRANSITION_TOTAL) {
                    phase = MultiRoomPhase.RETURNING_HOME;
                    view.hideTransition();
                }
                return true;
            }

            case RETURNING_HOME:
            default:
                return false;
        }
    }

    private void computeDoorPath() {
        List<Point> path = findPathBFS(robot.getX(), robot.getY(), DOOR_X, DOOR_Y);
        doorPath.clear();
        doorPath.addAll(path);
    }

    public void navigateToRoom(RoomType newTarget) {
        for (PlacedObstacle po : placedObstacles) {
            for (Point p : po.owned) {
                Cell c = room.getCell(p.x, p.y);
                if (c != null) c.setObstacle(false);
            }
        }
        dirtPath.clear();
        isSeekingDirt = false;
        returnPath.clear();
        doorPath.clear();
        isReturningToStation = false;
        view.setReturningStatus(false);
        view.hideTransition();
        transitionTimer = 0;
        view.clearUserObjects();
        placedObstacles.clear();

        wfVisited       = new boolean[20][14];
        wfSameAreaSteps = 0;
        wfEscaping      = false;
        wfEscapePath.clear();

        if (newTarget == RoomType.SALON) {
            if (room == salonRoom) {
                RoomLoader.clearDirt(salonRoom);
                RoomLoader.addInitialDirt(salonRoom, RoomType.SALON);
                targetRoomType = RoomType.SALON;
                phase = MultiRoomPhase.IN_SALON;
                view.setCurrentRoom(RoomType.SALON);
                detectUnreachableAreas();
                dirtyCount = countDirty(room);
                seekNextDirt();
            } else {
                targetRoomFullyCleaned = true;
                targetRoomType = RoomType.SALON;
                phase = MultiRoomPhase.HEADING_TO_EXIT;
                computeDoorPath();
            }
        } else {
            Room newRoom = new Room(20, 14);
            RoomLoader.loadLayout(newRoom, newTarget);
            RoomLoader.addInitialDirt(newRoom, newTarget);
            targetRoomObj          = newRoom;
            targetRoomFullyCleaned = false;
            targetRoomType         = newTarget;
            if (room == salonRoom) {
                phase = MultiRoomPhase.HEADING_TO_DOOR;
                computeDoorPath();
                view.setCurrentRoom(RoomType.SALON);
            } else {
                phase = MultiRoomPhase.HEADING_TO_EXIT;
                computeDoorPath();
            }
            dirtyCount = countDirty(newRoom);
        }

        cleanedCells = 0;
        detectUnreachableAreas();
        if (!isRunning) startSimulation();
    }

    private void updateStatistics() {
        elapsedTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long min = elapsedTimeInSeconds / 60;
        long sec = elapsedTimeInSeconds % 60;
        view.updateStats(cleanedCells, totalCleanableArea, unreachableAreaCount, dirtyCount,
                         String.format("%02d:%02d", min, sec));
    }

    private boolean isWalkableForRobot(int x, int y) {
        if (!room.isWalkable(x, y)) return false;
        if (room == salonRoom && cat.getX() == x && cat.getY() == y) return false;
        return true;
    }

    private void seekNextDirt() {
        Point nearest = findNearestDirt();
        if (nearest != null) {
            List<Point> path = findPathBFS(robot.getX(), robot.getY(), nearest.x, nearest.y);
            if (!path.isEmpty()) {
                dirtPath.clear();
                dirtPath.addAll(path);
                isSeekingDirt = true;
            }
        }
    }

    private Point findNearestDirt() {
        Queue<Point>  q   = new LinkedList<>();
        boolean[][]   vis = new boolean[20][14];
        q.add(new Point(robot.getX(), robot.getY()));
        vis[robot.getX()][robot.getY()] = true;
        int[] dx = {0,0,1,-1}, dy = {1,-1,0,0};
        while (!q.isEmpty()) {
            Point cur = q.poll();
            Cell  c   = room.getCell(cur.x, cur.y);
            if (c != null && c.hasDirt()) return cur;
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i], ny = cur.y + dy[i];
                if (nx >= 0 && nx < 20 && ny >= 0 && ny < 14 && !vis[nx][ny] && isWalkableForRobot(nx, ny)) {
                    vis[nx][ny] = true;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return null;
    }

    private void moveRandomly() {
        Point fwd = getNextPointForward();
        if (isWalkableForRobot(fwd.x, fwd.y)) { robot.move(fwd.x, fwd.y); return; }

        Direction r  = getRightDir(robot.getDirection());
        Point     rp = getPtInDir(robot.getX(), robot.getY(), r);
        if (isWalkableForRobot(rp.x, rp.y)) { robot.setDirection(r); robot.move(rp.x, rp.y); return; }

        Direction l  = getLeftDir(robot.getDirection());
        Point     lp = getPtInDir(robot.getX(), robot.getY(), l);
        if (isWalkableForRobot(lp.x, lp.y)) { robot.setDirection(l); robot.move(lp.x, lp.y); return; }

        Direction b  = getRightDir(getRightDir(robot.getDirection()));
        Point     bp = getPtInDir(robot.getX(), robot.getY(), b);
        if (isWalkableForRobot(bp.x, bp.y)) { robot.setDirection(b); robot.move(bp.x, bp.y); }
    }

    private void moveSpiral() {
        if (spiralStepsTaken < spiralStepsTarget) {
            Point next = getNextPointForward();
            if (isWalkableForRobot(next.x, next.y)) {
                robot.move(next.x, next.y);
                spiralStepsTaken++;
            } else {
                turnRight();
                spiralStepsTaken = 0;
                spiralTurns      = 0;
                spiralStepsTarget = 1;
            }
        } else {
            turnRight();
            spiralStepsTaken = 0;
            spiralTurns++;
            if (spiralTurns % 2 == 0) spiralStepsTarget++;
        }
    }

    private void moveWallFollowing() {

        if (wfEscaping) {
            if (!wfEscapePath.isEmpty()) {
                moveToPoint(wfEscapePath.poll());
                return;
            }
            wfEscaping      = false;
            wfSameAreaSteps = 0;
        }

        Direction right   = getRightDir(robot.getDirection());
        Direction forward = robot.getDirection();
        Direction left    = getLeftDir(robot.getDirection());
        Direction back    = getRightDir(getRightDir(robot.getDirection()));

        Point rp = getPtInDir(robot.getX(), robot.getY(), right);
        Point fp = getPtInDir(robot.getX(), robot.getY(), forward);
        Point lp = getPtInDir(robot.getX(), robot.getY(), left);
        Point bp = getPtInDir(robot.getX(), robot.getY(), back);

        if      (isWalkableForRobot(rp.x, rp.y)) { robot.setDirection(right); robot.move(rp.x, rp.y); }
        else if (isWalkableForRobot(fp.x, fp.y))  { robot.move(fp.x, fp.y); }
        else if (isWalkableForRobot(lp.x, lp.y))  { robot.setDirection(left);  robot.move(lp.x, lp.y); }
        else if (isWalkableForRobot(bp.x, bp.y))  { robot.setDirection(back);  robot.move(bp.x, bp.y); }

        int rx = robot.getX(), ry = robot.getY();
        if (wfVisited[rx][ry]) {
            wfSameAreaSteps++;
            if (wfSameAreaSteps >= WF_ESCAPE_THRESHOLD) {
                Point target = findNearestUnvisitedWF();
                if (target != null) {
                    List<Point> path = findPathBFS(rx, ry, target.x, target.y);
                    if (!path.isEmpty()) {
                        wfEscapePath.clear();
                        wfEscapePath.addAll(path);
                        wfEscaping      = true;
                        wfSameAreaSteps = 0;
                    } else {
                        wfSameAreaSteps = 0;
                    }
                } else {
                    wfSameAreaSteps = 0;
                }
            }
        } else {
            wfVisited[rx][ry] = true;
            wfSameAreaSteps   = 0;
        }
    }

    private Point findNearestUnvisitedWF() {
        Queue<Point> q   = new LinkedList<>();
        boolean[][]  vis = new boolean[20][14];
        q.add(new Point(robot.getX(), robot.getY()));
        vis[robot.getX()][robot.getY()] = true;
        int[] dx = {0,0,1,-1}, dy = {1,-1,0,0};
        while (!q.isEmpty()) {
            Point cur = q.poll();
            if (!wfVisited[cur.x][cur.y]) return cur;
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i], ny = cur.y + dy[i];
                if (nx >= 0 && nx < 20 && ny >= 0 && ny < 14 && !vis[nx][ny] && isWalkableForRobot(nx, ny)) {
                    vis[nx][ny] = true;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return null;
    }

    private void moveToPoint(Point p) {
        if      (p.x > robot.getX()) robot.setDirection(Direction.EAST);
        else if (p.x < robot.getX()) robot.setDirection(Direction.WEST);
        else if (p.y > robot.getY()) robot.setDirection(Direction.SOUTH);
        else if (p.y < robot.getY()) robot.setDirection(Direction.NORTH);
        robot.move(p.x, p.y);
    }

    private Point getNextPointForward() {
        return getPtInDir(robot.getX(), robot.getY(), robot.getDirection());
    }

    private Point getPtInDir(int x, int y, Direction d) {
        int nx = x, ny = y;
        switch (d) {
            case NORTH: ny--; break;
            case SOUTH: ny++; break;
            case EAST:  nx++; break;
            case WEST:  nx--; break;
        }
        return new Point(nx, ny);
    }

    private void turnRight() { robot.setDirection(getRightDir(robot.getDirection())); }

    private Direction getRightDir(Direction d) {
        switch (d) {
            case NORTH: return Direction.EAST;
            case EAST:  return Direction.SOUTH;
            case SOUTH: return Direction.WEST;
            default:    return Direction.NORTH;
        }
    }

    private Direction getLeftDir(Direction d) {
        switch (d) {
            case NORTH: return Direction.WEST;
            case WEST:  return Direction.SOUTH;
            case SOUTH: return Direction.EAST;
            default:    return Direction.NORTH;
        }
    }

    private void detectUnreachableAreas() {
        boolean[][] vis = new boolean[20][14];
        Queue<Point> q  = new LinkedList<>();
        int sx = robot.getX(), sy = robot.getY();
        q.add(new Point(sx, sy));
        vis[sx][sy] = true;
        int reach = 0, obs = 0, reachDirty = 0;
        int[] dx = {0,0,1,-1}, dy = {1,-1,0,0};

        while (!q.isEmpty()) {
            Point cur = q.poll();
            reach++;
            Cell c = room.getCell(cur.x, cur.y);
            if (c != null && c.hasDirt()) reachDirty++;
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i], ny = cur.y + dy[i];
                if (nx >= 0 && nx < 20 && ny >= 0 && ny < 14 && !vis[nx][ny] && room.isWalkable(nx, ny)) {
                    vis[nx][ny] = true;
                    q.add(new Point(nx, ny));
                }
            }
        }

        for (int x = 0; x < 20; x++) for (int y = 0; y < 14; y++) if (!room.isWalkable(x, y)) obs++;
        totalCleanableArea    = (20 * 14) - obs;
        unreachableAreaCount  = totalCleanableArea - reach;
        dirtyCount            = reachDirty;
    }

    private List<Point> findPathBFS(int sx, int sy, int tx, int ty) {
        Queue<Point>     q   = new LinkedList<>();
        boolean[][]      vis = new boolean[20][14];
        Map<Point, Point> par = new HashMap<>();
        q.add(new Point(sx, sy));
        vis[sx][sy] = true;
        int[] dx = {0,0,1,-1}, dy = {1,-1,0,0};

        while (!q.isEmpty()) {
            Point cur = q.poll();
            if (cur.x == tx && cur.y == ty) return rebuildPath(par, cur);
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i], ny = cur.y + dy[i];
                if (nx >= 0 && nx < 20 && ny >= 0 && ny < 14 && !vis[nx][ny] && isWalkableForRobot(nx, ny)) {
                    Point nb = new Point(nx, ny);
                    vis[nx][ny] = true;
                    par.put(nb, cur);
                    q.add(nb);
                }
            }
        }
        return new ArrayList<>();
    }

    private List<Point> rebuildPath(Map<Point, Point> par, Point cur) {
        List<Point> path = new ArrayList<>();
        while (par.containsKey(cur)) { path.add(cur); cur = par.get(cur); }
        Collections.reverse(path);
        return path;
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() { return Objects.hash(x, y); }
    }
}
