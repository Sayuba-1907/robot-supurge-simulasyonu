package com.example.robotvacuumsimulation;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimulationView extends BorderPane {

    private Pane   canvasPane;
    private Canvas canvas;
    private GraphicsContext gc;
    private int    gridWidth;
    private int    gridHeight;

    private Button btnAddDirt, btnAddObstacle, btnRemoveFurniture;
    private Button btnStart, btnPause, btnReturn, btnReset;
    private Button btnGoSalon, btnGoBedroom, btnGoOffice;
    private Consumer<RoomType> onRoomNavigate;

    private RadioButton rbDust, rbLiquid, rbStain;
    private ToggleGroup dirtGroup;

    private Label lblBattery, lblTotalArea, lblCleanedArea, lblDirtyArea, lblTime, lblUnreachable;
    private Label lblRobotLocation, lblRobotDirection;
    private Label lblCurrentRoom;

    private Slider      sliderSpeed;
    private Slider      sliderBattery;
    private ProgressBar pbBattery;

    private RadioButton rbRandom, rbSpiral, rbWallFollow;
    private ToggleGroup algoGroup;

    private RadioButton rbWall, rbSofa, rbTable;
    private ToggleGroup obsGroup;

    private List<double[]> pathHistory = new ArrayList<>();
    private boolean        isReturning = false;

    private Pet      cat;
    private RoomType currentRoomType = RoomType.SALON;

    private boolean  cleaningAnimActive     = false;
    private DirtType cleaningAnimDirt       = null;
    private int      cleaningAnimFrame      = 0;
    private int      cleaningAnimMaxFrames  = 6;
    private int      cleaningAnimX          = 0;
    private int      cleaningAnimY          = 0;

    private boolean transitionActive = false;
    private String  transitionText   = "";
    private int     transitionFrame  = 0;

    public static class PlacedObj {
        int x, y, w, h; String type;
        public PlacedObj(int x, int y, int w, int h, String type) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.type = type;
        }
    }
    private List<PlacedObj> userObjects = new ArrayList<>();

    private Image imgRobot, imgWood, imgFurniture, imgStation, imgDust, imgLiquid, imgStain, imgPlant;

    public SimulationView(int width, int height) {
        this.gridWidth  = width;
        this.gridHeight = height;
        loadImages();
        canvasPane = new Pane();
        canvasPane.setStyle("-fx-background-color: #0d1117;");
        canvas = new Canvas();
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvasPane.getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();
        this.setCenter(canvasPane);
        createTopPanel();
        createLeftPanel();
        createBottomPanel();
    }

    private void loadImages() {
        imgRobot     = loadImg("/robot.png");
        imgWood      = loadImg("/wood.png");
        imgFurniture = loadImg("/furniture.png");
        imgStation   = loadImg("/station.png");
        imgDust      = loadImg("/dust.png");
        imgLiquid    = loadImg("/liquid.png");
        imgStain     = loadImg("/stain.png");
        imgPlant     = loadImg("/plant.png");
    }

    private Image loadImg(String path) {
        try { var res = getClass().getResource(path); if (res != null) return new Image(res.toExternalForm()); }
        catch (Exception ignored) {}
        return null;
    }

    public void showTransition(String text) { transitionActive = true; transitionText = text; transitionFrame = 0; }
    public void hideTransition() { transitionActive = false; }

    public void setCurrentRoom(RoomType type) {
        this.currentRoomType = type;
        String roomLabel = switch (type) {
            case SALON   -> "Salon";
            case BEDROOM -> "Yatak Odasi";
            case OFFICE  -> "Ofis";
        };
        String color = switch (type) {
            case SALON   -> "#3fb950";
            case BEDROOM -> "#a371f7";
            case OFFICE  -> "#d29922";
        };
        if (lblCurrentRoom != null) {
            lblCurrentRoom.setText("Oda: " + roomLabel);
            lblCurrentRoom.setTextFill(Color.web(color));
        }
        updateRoomNavButtons(type);
    }

    public void setOnRoomNavigate(Consumer<RoomType> handler) { this.onRoomNavigate = handler; }

    private void createTopPanel() {
        BorderPane top = new BorderPane();
        top.setPadding(new Insets(12, 30, 12, 30));
        top.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        HBox leftBox = new HBox(10);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        if (imgRobot != null) {
            ImageView iv = new ImageView(imgRobot);
            iv.setFitWidth(36); iv.setFitHeight(36); iv.setPreserveRatio(true);
            iv.setStyle("-fx-effect: dropshadow(gaussian, #58a6ff, 8, 0.4, 0, 0);");
            leftBox.getChildren().add(iv);
        }
        Label icon = new Label("Robot");
        icon.setFont(Font.font("System", FontWeight.BOLD, 22));
        icon.setTextFill(Color.web("#58a6ff"));
        leftBox.getChildren().add(icon);
        top.setLeft(leftBox);
        BorderPane.setAlignment(leftBox, Pos.CENTER_LEFT);

        Label title = new Label("Robot Supurge Simulasyonu");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        top.setCenter(title);
        BorderPane.setAlignment(title, Pos.CENTER);

        lblCurrentRoom = new Label("Oda: Salon");
        lblCurrentRoom.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblCurrentRoom.setTextFill(Color.web("#3fb950"));
        lblCurrentRoom.setStyle("-fx-background-color: #21262d; -fx-background-radius: 6; -fx-padding: 4 10;");
        top.setRight(lblCurrentRoom);
        BorderPane.setAlignment(lblCurrentRoom, Pos.CENTER_RIGHT);

        this.setTop(top);
    }

    private void createLeftPanel() {
        VBox leftPanel = new VBox(12);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        leftPanel.setMinWidth(260); leftPanel.setPrefWidth(260);

        String base   = "-fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 8 12; -fx-cursor: hand; -fx-font-size: 12px;";
        String blue   = "-fx-background-color: #1f6feb;" + base;
        String green  = "-fx-background-color: #2ea043;" + base;
        String red    = "-fx-background-color: #da3633;" + base;
        String dark   = "-fx-background-color: #21262d; -fx-border-color: #30363d; -fx-border-radius: 6;" + base;
        String cyan   = "-fx-background-color: #086b62;" + base;
        String orange = "-fx-background-color: #9a5f00;" + base;

        Label lTools = new Label("Araclar"); lTools.setFont(Font.font("System", FontWeight.BOLD, 13)); lTools.setTextFill(Color.WHITE);

        btnAddDirt = new Button("Kir Ekle"); btnAddDirt.setStyle(blue); btnAddDirt.setMaxWidth(Double.MAX_VALUE);

        Label lDirt = new Label("Kir Turu:"); lDirt.setTextFill(Color.web("#8b949e")); lDirt.setFont(Font.font(11));
        dirtGroup = new ToggleGroup();
        rbDust = new RadioButton("Toz"); rbLiquid = new RadioButton("Sivi"); rbStain = new RadioButton("Leke");
        rbDust.setTextFill(Color.WHITE); rbLiquid.setTextFill(Color.WHITE); rbStain.setTextFill(Color.WHITE);
        rbDust.setToggleGroup(dirtGroup); rbLiquid.setToggleGroup(dirtGroup); rbStain.setToggleGroup(dirtGroup);
        rbDust.setSelected(true);
        HBox dirtBox = new HBox(10, rbDust, rbLiquid, rbStain);

        btnAddObstacle = new Button("Mobilya Ekle"); btnAddObstacle.setStyle(green); btnAddObstacle.setMaxWidth(Double.MAX_VALUE);
        btnRemoveFurniture = new Button("Mobilya Sil"); btnRemoveFurniture.setStyle(orange); btnRemoveFurniture.setMaxWidth(Double.MAX_VALUE);

        obsGroup = new ToggleGroup();
        rbWall = new RadioButton("Duvar"); rbSofa = new RadioButton("Koltuk"); rbTable = new RadioButton("Masa");
        rbWall.setTextFill(Color.WHITE); rbSofa.setTextFill(Color.WHITE); rbTable.setTextFill(Color.WHITE);
        rbWall.setToggleGroup(obsGroup); rbSofa.setToggleGroup(obsGroup); rbTable.setToggleGroup(obsGroup);
        rbSofa.setSelected(true);
        HBox obsBox = new HBox(8, rbWall, rbSofa, rbTable);

        Label lSpeed = new Label("Robot Hizi:"); lSpeed.setTextFill(Color.web("#8b949e")); lSpeed.setFont(Font.font(11));
        sliderSpeed = new Slider(0.5, 3.0, 1.0); sliderSpeed.setShowTickMarks(true); sliderSpeed.setShowTickLabels(true); sliderSpeed.setMajorTickUnit(0.5);

        Label lBat = new Label("Manuel Batarya:"); lBat.setTextFill(Color.web("#8b949e")); lBat.setFont(Font.font(11));
        sliderBattery = new Slider(0, 100, 100); sliderBattery.setShowTickMarks(true); sliderBattery.setShowTickLabels(true); sliderBattery.setMajorTickUnit(25);

        Label lAlgo = new Label("Algoritma:"); lAlgo.setTextFill(Color.web("#8b949e")); lAlgo.setFont(Font.font(11));
        algoGroup = new ToggleGroup();
        rbRandom = new RadioButton("Rastgele"); rbSpiral = new RadioButton("Spiral"); rbWallFollow = new RadioButton("Kenar Takibi");
        rbRandom.setTextFill(Color.WHITE); rbSpiral.setTextFill(Color.WHITE); rbWallFollow.setTextFill(Color.WHITE);
        rbRandom.setToggleGroup(algoGroup); rbSpiral.setToggleGroup(algoGroup); rbWallFollow.setToggleGroup(algoGroup);
        rbRandom.setSelected(true);
        HBox algoBox = new HBox(8, rbRandom, rbSpiral, rbWallFollow);

        Label lRobotTitle = new Label("Robot Durumu"); lRobotTitle.setFont(Font.font("System", FontWeight.BOLD, 13)); lRobotTitle.setTextFill(Color.WHITE);
        HBox locBox = new HBox(); Label lL1 = new Label("Konum:"); lL1.setTextFill(Color.web("#8b949e")); lL1.setFont(Font.font(11));
        lblRobotLocation = new Label("(11, 7)"); lblRobotLocation.setTextFill(Color.WHITE); lblRobotLocation.setFont(Font.font(11));
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS); locBox.getChildren().addAll(lL1, sp1, lblRobotLocation);
        HBox dirBox = new HBox(); Label lD1 = new Label("Yon:"); lD1.setTextFill(Color.web("#8b949e")); lD1.setFont(Font.font(11));
        lblRobotDirection = new Label("Dogu"); lblRobotDirection.setTextFill(Color.web("#3fb950")); lblRobotDirection.setFont(Font.font(11));
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS); dirBox.getChildren().addAll(lD1, sp2, lblRobotDirection);
        HBox batBox = new HBox(); Label lB1 = new Label("Batarya:"); lB1.setTextFill(Color.web("#8b949e")); lB1.setFont(Font.font(11));
        lblBattery = new Label("%100"); lblBattery.setTextFill(Color.web("#d29922")); lblBattery.setFont(Font.font(11));
        Region sp3 = new Region(); HBox.setHgrow(sp3, Priority.ALWAYS); batBox.getChildren().addAll(lB1, sp3, lblBattery);
        pbBattery = new ProgressBar(1.0); pbBattery.setMaxWidth(Double.MAX_VALUE); pbBattery.setPrefHeight(10);
        pbBattery.setStyle("-fx-accent: #d29922; -fx-control-inner-background: #21262d;");
        VBox statusBox = new VBox(4, lRobotTitle, locBox, dirBox, batBox, pbBattery);

        Label lCtrl = new Label("Kontroller"); lCtrl.setFont(Font.font("System", FontWeight.BOLD, 13)); lCtrl.setTextFill(Color.WHITE);
        btnStart  = new Button("Baslat");   btnStart.setStyle(blue); btnStart.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(btnStart, Priority.ALWAYS);
        btnPause  = new Button("Duraklat"); btnPause.setStyle(dark); btnPause.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(btnPause, Priority.ALWAYS);
        HBox spBox = new HBox(8, btnStart, btnPause);
        btnReset  = new Button("Sifirla");       btnReset.setStyle(red);  btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReturn = new Button("Istasyona Don"); btnReturn.setStyle(cyan); btnReturn.setMaxWidth(Double.MAX_VALUE);

        Label lRoom = new Label("Oda Gecisi"); lRoom.setFont(Font.font("System", FontWeight.BOLD, 13)); lRoom.setTextFill(Color.WHITE);
        btnGoSalon   = makeRoomNavBtn("Salon",       "#3fb950", "#0d2b18");
        btnGoBedroom = makeRoomNavBtn("Yatak Odasi", "#a371f7", "#1e1033");
        btnGoOffice  = makeRoomNavBtn("Ofis",        "#d29922", "#2b2005");
        btnGoSalon.setOnAction(e ->   { if (onRoomNavigate != null) onRoomNavigate.accept(RoomType.SALON); });
        btnGoBedroom.setOnAction(e -> { if (onRoomNavigate != null) onRoomNavigate.accept(RoomType.BEDROOM); });
        btnGoOffice.setOnAction(e ->  { if (onRoomNavigate != null) onRoomNavigate.accept(RoomType.OFFICE); });
        HBox.setHgrow(btnGoSalon, Priority.ALWAYS); HBox.setHgrow(btnGoBedroom, Priority.ALWAYS); HBox.setHgrow(btnGoOffice, Priority.ALWAYS);
        HBox roomRow = new HBox(6, btnGoSalon, btnGoBedroom, btnGoOffice); roomRow.setMaxWidth(Double.MAX_VALUE);
        updateRoomNavButtons(RoomType.SALON);

        leftPanel.getChildren().addAll(
            lTools, btnAddDirt, lDirt, dirtBox,
            btnAddObstacle, obsBox, btnRemoveFurniture, new Separator(),
            lSpeed, sliderSpeed, lBat, sliderBattery, new Separator(),
            lAlgo, algoBox, new Separator(),
            lRoom, roomRow, new Separator(),
            statusBox, new Separator(),
            lCtrl, spBox, btnReset, btnReturn
        );
        ScrollPane sp = new ScrollPane(leftPanel);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #161b22; -fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.setLeft(sp);
    }

    private void createBottomPanel() {
        HBox bottom = new HBox(20);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(15, 30, 15, 30));
        bottom.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1 0 0 0;");
        VBox b1 = mkStat("Toplam Alan",  "260 m²",   "#58a6ff");
        VBox b2 = mkStat("Temizlenen",   "0 m2 %0",  "#3fb950");
        VBox b3 = mkStat("Kirli Alan",   "0 hucre",  "#f0883e");
        VBox b4 = mkStat("Ulasilamayan", "0 m2",     "#f85149");
        VBox b5 = mkStat("Gecen Sure",   "00:00",    "#a371f7");
        lblTotalArea   = (Label) b1.getChildren().get(1);
        lblCleanedArea = (Label) b2.getChildren().get(1);
        lblDirtyArea   = (Label) b3.getChildren().get(1);
        lblUnreachable = (Label) b4.getChildren().get(1);
        lblTime        = (Label) b5.getChildren().get(1);
        bottom.getChildren().addAll(b1, b2, b3, b4, b5);
        this.setBottom(bottom);
    }

    private VBox mkStat(String title, String val, String color) {
        Label t = new Label(title); t.setTextFill(Color.web(color)); t.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label v = new Label(val);   v.setTextFill(Color.WHITE);      v.setFont(Font.font("System", FontWeight.BOLD, 14));
        VBox box = new VBox(5, t, v); box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #21262d; -fx-background-radius: 8; -fx-padding: 8 20;");
        return box;
    }

    private boolean isPredefinedFurniture(int x, int y) {
        if (currentRoomType == RoomType.SALON)   return isSalonFurniture(x, y);
        if (currentRoomType == RoomType.BEDROOM) return isBedroomFurniture(x, y);
        if (currentRoomType == RoomType.OFFICE)  return isOfficeFurniture(x, y);
        return false;
    }

    private boolean isSalonFurniture(int x, int y) {
        if (x >= 7  && x < 13 && y >= 1 && y < 3)  return true;
        if (x >= 2  && x < 4  && y >= 4 && y < 10) return true;
        if (x >= 8  && x < 11 && y >= 6 && y < 9)  return true;
        if (x >= 15 && x < 18 && y >= 6 && y < 9)  return true;
        if (x >= 15 && x < 18 && y == 1)            return true;
        if (x == 18 && y >= 9 && y < 12)            return true;
        if (x == 1  && y == 1)                       return true;
        if (x == 18 && y == 1)                       return true;
        return false;
    }

    private boolean isBedroomFurniture(int x, int y) {
        if (x >= 7  && x < 11 && y >= 2 && y < 5)  return true;
        if (x >= 2  && x < 4  && y >= 2 && y < 6)  return true;
        if (x >= 16 && x < 18 && y >= 2 && y < 6)  return true;
        if (x == 6  && y == 2)                       return true;
        if (x == 12 && y == 2)                       return true;
        if (x >= 2  && x < 5  && y >= 9 && y < 11) return true;
        if (x >= 15 && x < 18 && y >= 9 && y < 11) return true;
        return false;
    }

    private boolean isOfficeFurniture(int x, int y) {
        if (x == 1  && y == 1)                        return true;
        if (x == 18 && y == 1)                        return true;
        if (x >= 2  && x < 5  && y >= 2  && y < 4)  return true;
        if (x >= 2  && x < 5  && y >= 7  && y < 9)  return true;
        if (x >= 6  && x < 9  && y >= 2  && y < 4)  return true;
        if (x >= 6  && x < 9  && y >= 7  && y < 9)  return true;
        if (x >= 12 && x < 15 && y >= 2  && y < 4)  return true;
        if (x >= 12 && x < 15 && y >= 7  && y < 9)  return true;
        if (x >= 16 && x < 18 && y >= 2  && y < 4)  return true;
        if (x >= 16 && x < 18 && y >= 7  && y < 9)  return true;
        if (x == 10 && y >= 1 && y < 4)              return true;
        if (x == 10 && y >= 5 && y < 10)             return true;
        if (x >= 2  && x < 5  && y >= 10 && y < 12) return true;
        if (x >= 14 && x < 17 && y >= 10 && y < 12) return true;
        return false;
    }

    private boolean isUserFurnitureCell(int x, int y) {
        for (PlacedObj obj : userObjects) {
            if (!obj.type.equals("WALL") && x >= obj.x && x < obj.x + obj.w && y >= obj.y && y < obj.y + obj.h)
                return true;
        }
        return false;
    }

    public double[] getGridOffsetAndSize() {
        double cw = canvas.getWidth(); double ch = canvas.getHeight();
        double cellSize = Math.min((cw - 40) / gridWidth, (ch - 40) / gridHeight);
        double offsetX  = (cw - (gridWidth  * cellSize)) / 2.0 + 15;
        double offsetY  = (ch - (gridHeight * cellSize)) / 2.0 + 15;
        return new double[]{offsetX, offsetY, cellSize};
    }

    public void render(Room room, Robot robot) {
        double cw = canvas.getWidth(); double ch = canvas.getHeight();
        if (cw <= 0 || ch <= 0) return;
        gc.clearRect(0, 0, cw, ch);
        double[] metrics = getGridOffsetAndSize();
        double oX = metrics[0], oY = metrics[1], cs = metrics[2];

        gc.setFill(Color.web("#8b949e")); gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int x = 0; x < gridWidth; x++) gc.fillText(String.valueOf(x), oX + x*cs + cs/2, oY - 8);
        gc.setTextAlign(TextAlignment.RIGHT);
        for (int y = 0; y < gridHeight; y++) gc.fillText(String.valueOf(y), oX - 8, oY + y*cs + cs/2 + 4);
        gc.setTextAlign(TextAlignment.LEFT);

        gc.setFill(Color.web("#f0e3d1")); gc.fillRect(oX, oY, gridWidth*cs, gridHeight*cs);
        gc.setStroke(Color.web("#dfd0bc")); gc.setLineWidth(1);
        for (int x = 1; x < gridWidth; x++)  gc.strokeLine(oX+x*cs, oY, oX+x*cs, oY+gridHeight*cs);
        for (int y = 1; y < gridHeight; y++) gc.strokeLine(oX, oY+y*cs, oX+gridWidth*cs, oY+y*cs);

        double wd = cs * 0.35;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = room.getCell(x, y);
                double px = oX + x*cs, py = oY + y*cs;
                if (cell.isObstacle() && !isPredefinedFurniture(x, y) && !isUserFurnitureCell(x, y)) {
                    boolean sO = (y < gridHeight-1) && !room.getCell(x, y+1).isObstacle();
                    boolean eO = (x < gridWidth-1)  && !room.getCell(x+1, y).isObstacle();
                    boolean nO = (y > 0)             && !room.getCell(x, y-1).isObstacle();
                    boolean wO = (x > 0)             && !room.getCell(x-1, y).isObstacle();
                    gc.setFill(Color.web("#7b8898")); gc.fillRect(px, py, cs, cs);
                    if (sO) { gc.setFill(Color.web("#1a2332")); gc.fillRect(px, py+cs-wd, cs, wd); gc.setStroke(Color.web("#b0bac8")); gc.setLineWidth(1); gc.strokeLine(px, py+cs-wd, px+cs, py+cs-wd); }
                    if (eO) { gc.setFill(Color.web("#2d3748")); gc.fillRect(px+cs-wd*0.6, py, wd*0.6, cs); }
                    if (nO) { gc.setFill(Color.web("#b0bac8")); gc.fillRect(px, py, cs, 2); }
                    if (wO) { gc.setFill(Color.web("#b0bac8")); gc.fillRect(px, py, 2, cs); }
                    gc.setStroke(Color.web("#374151", 0.45)); gc.setLineWidth(0.7);
                    double my = py+cs*0.5;
                    gc.strokeLine(px, my, px+cs, my);
                    gc.strokeLine(px+cs*0.5, py, px+cs*0.5, my); gc.strokeLine(px+cs*0.25, my, px+cs*0.25, py+cs); gc.strokeLine(px+cs*0.75, my, px+cs*0.75, py+cs);
                }
            }
        }

        if (currentRoomType == RoomType.SALON) {
            gc.setFill(Color.web("#238636")); gc.fillRect(oX+cs, oY+12*cs, cs, cs);
            if (imgStation != null) gc.drawImage(imgStation, oX+0.5*cs, oY+11.5*cs, 2*cs, 1.5*cs);
        }

        for (int x = 1; x < gridWidth-1; x++) {
            for (int y = 1; y < gridHeight-1; y++) {
                Cell cell = room.getCell(x, y); double px = oX+x*cs, py = oY+y*cs;
                if (cell.hasDirt() && !isPredefinedFurniture(x, y)) {
                    Image dImg = cell.getDirt()==DirtType.DUST?imgDust : cell.getDirt()==DirtType.LIQUID?imgLiquid : imgStain;
                    if (dImg != null) { double ds=cs*0.6, dof=(cs-ds)/2; gc.drawImage(dImg, px+dof, py+dof, ds, ds); }
                    else { gc.setFill(cell.getDirt()==DirtType.DUST?Color.web("#8c8c8c"):cell.getDirt()==DirtType.LIQUID?Color.web("#58a6ff"):Color.web("#da3633")); gc.fillOval(px+cs/3, py+cs/3, cs/3, cs/3); }
                }
            }
        }

        if      (currentRoomType == RoomType.SALON)   drawSalonFurniture(gc, oX, oY, cs);
        else if (currentRoomType == RoomType.BEDROOM) drawBedroomFurniture(gc, oX, oY, cs);
        else if (currentRoomType == RoomType.OFFICE)  drawOfficeFurniture(gc, oX, oY, cs);

        for (PlacedObj obj : userObjects) {
            double ox2=oX+obj.x*cs, oy2=oY+obj.y*cs, ow=obj.w*cs, oh=obj.h*cs;
            if (obj.type.equals("SOFA")) drawSofa(gc, ox2, oy2, ow, oh);
            else if (!obj.type.equals("WALL")) { if (imgWood!=null) gc.drawImage(imgWood, ox2, oy2, ow, oh); else { gc.setFill(Color.web("#8b6914")); gc.fillRect(ox2,oy2,ow,oh); } }
        }

        if (pathHistory.isEmpty() || pathHistory.get(pathHistory.size()-1)[0]!=robot.getX() || pathHistory.get(pathHistory.size()-1)[1]!=robot.getY()) {
            pathHistory.add(new double[]{robot.getX(), robot.getY()});
            if (pathHistory.size() > 40) pathHistory.remove(0);
        }
        if (pathHistory.size() > 1) {
            Color pc = isReturning ? Color.web("#3fb950") : Color.web("#3498db");
            gc.setStroke(pc); gc.setLineWidth(2.5); gc.setLineDashes(8); gc.beginPath();
            for (int i = 0; i < pathHistory.size(); i++) { double[] p=pathHistory.get(i); double px2=oX+p[0]*cs+cs/2, py2=oY+p[1]*cs+cs/2; if(i==0)gc.moveTo(px2,py2);else gc.lineTo(px2,py2); }
            gc.stroke(); gc.setLineDashes(0);
            for (int i=1;i<pathHistory.size();i++) { if(i%4==0){double[] pv=pathHistory.get(i-1);double[] pc2=pathHistory.get(i); drawArrowHead(gc,oX+pv[0]*cs+cs/2,oY+pv[1]*cs+cs/2,oX+pc2[0]*cs+cs/2,oY+pc2[1]*cs+cs/2,pc);} }
        }

        if (cat != null && currentRoomType == RoomType.SALON) drawCat(gc, oX+cat.getX()*cs, oY+cat.getY()*cs, cs);

        double rx=oX+robot.getX()*cs, ry=oY+robot.getY()*cs;
        if (imgRobot != null) {
            gc.save(); double cx2=rx+cs/2, cy2=ry+cs/2; gc.translate(cx2,cy2);
            switch(robot.getDirection()){case NORTH:gc.rotate(-90);break;case SOUTH:gc.rotate(90);break;case WEST:gc.rotate(180);break;default:break;}
            double rs=cs*0.95; gc.drawImage(imgRobot,-rs/2,-rs/2,rs,rs); gc.restore();
        } else { gc.setFill(Color.web("#21262d")); gc.fillOval(rx+2,ry+2,cs-4,cs-4); }

        if (cleaningAnimActive) {
            double ax=oX+cleaningAnimX*cs+cs/2, ay=oY+cleaningAnimY*cs+cs/2;
            double prog=(double)cleaningAnimFrame/cleaningAnimMaxFrames;
            Color rc, gc2;
            if(cleaningAnimDirt==DirtType.DUST){rc=Color.web("#c8b06a");gc2=Color.web("#ffe08a");}
            else if(cleaningAnimDirt==DirtType.LIQUID){rc=Color.web("#58a6ff");gc2=Color.web("#a5d6ff");}
            else{rc=Color.web("#f47067");gc2=Color.web("#ffb0a8");}
            for(int ring=0;ring<3;ring++){double rp=Math.min(1.0,prog+ring*0.15);double rad=cs*0.55*rp;double al=(1-rp)*0.85; gc.setStroke(Color.color(rc.getRed(),rc.getGreen(),rc.getBlue(),al)); gc.setLineWidth(2.5-ring*0.6); gc.strokeOval(ax-rad,ay-rad,rad*2,rad*2);}
            for(int s=0;s<6;s++){double ang=Math.toRadians(s*60+prog*300);double dist=cs*0.42*prog; double sx2=ax+Math.cos(ang)*dist,sy2=ay+Math.sin(ang)*dist; double sa=(1-prog)*0.9,ss=cs*0.09*(1-prog*0.4); gc.setFill(Color.color(gc2.getRed(),gc2.getGreen(),gc2.getBlue(),sa)); gc.fillOval(sx2-ss/2,sy2-ss/2,ss,ss);}
            if(prog<0.55){double gl=cs*0.28*(1-prog/0.55);double gla=0.55*(1-prog/0.55); gc.setFill(Color.color(gc2.getRed(),gc2.getGreen(),gc2.getBlue(),gla)); gc.fillOval(ax-gl,ay-gl,gl*2,gl*2);}
            cleaningAnimFrame++; if(cleaningAnimFrame>=cleaningAnimMaxFrames)cleaningAnimActive=false;
        }

        if (transitionActive) {
            transitionFrame++; double alpha=Math.min(0.88,transitionFrame*0.30);
            gc.setFill(Color.color(0.05,0.07,0.10,alpha)); gc.fillRect(0,0,cw,ch);
            if(transitionFrame>2){double ta=Math.min(1.0,(transitionFrame-2)/3.0); double cx3=cw/2,cy3=ch/2-20;
            gc.setFill(Color.color(0.12,0.16,0.22,ta*0.85)); gc.fillOval(cx3-36,cy3-36,72,72);
            gc.setFill(Color.color(1,1,1,ta)); gc.setFont(Font.font("System",FontWeight.BOLD,22)); gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(transitionText,cx3,cy3+45); gc.setFont(Font.font("System",14)); gc.setFill(Color.color(0.7,0.75,0.80,ta)); gc.fillText("Robot ilerliyor...",cx3,cy3+72);}
        }

        lblRobotLocation.setText(String.format("(%d, %d)", robot.getX(), robot.getY()));
        String dt="Dogu",da="->"; Color dc=Color.web("#3fb950");
        switch(robot.getDirection()){case NORTH:dt="Kuzey";da="^";dc=Color.web("#58a6ff");break;case SOUTH:dt="Guney";da="v";dc=Color.web("#f85149");break;case WEST:dt="Bati";da="<";dc=Color.web("#d29922");break;}
        lblRobotDirection.setText(da+" "+dt); lblRobotDirection.setTextFill(dc);
        lblBattery.setText(String.format("%%%d",(int)robot.getBattery())); pbBattery.setProgress(robot.getBattery()/100.0);
        if(robot.getBattery()>50) pbBattery.setStyle("-fx-accent: #3fb950; -fx-control-inner-background: #2b313b;");
        else if(robot.getBattery()>20) pbBattery.setStyle("-fx-accent: #d29922; -fx-control-inner-background: #2b313b;");
        else pbBattery.setStyle("-fx-accent: #f85149; -fx-control-inner-background: #2b313b;");
    }

    private void drawSalonFurniture(GraphicsContext gc, double oX, double oY, double cs) {
        if(imgPlant!=null){gc.drawImage(imgPlant,oX+cs,oY+cs,cs,cs);gc.drawImage(imgPlant,oX+18*cs,oY+cs,cs,cs);}
        drawSofa(gc,oX+7*cs,oY+cs,6*cs,2*cs); drawSofa(gc,oX+2*cs,oY+4*cs,2*cs,6*cs);
        if(imgWood!=null){gc.drawImage(imgWood,oX+15*cs,oY+6*cs,3*cs,3*cs);gc.drawImage(imgWood,oX+8*cs,oY+6*cs,3*cs,3*cs);}
        double tvX=oX+15*cs,tvY=oY+cs,tvW=3*cs,tvH=cs;
        gc.setFill(Color.web("#1c1c1c"));gc.fillRoundRect(tvX,tvY,tvW,tvH,6,6);
        gc.setFill(Color.web("#0a0a0a"));gc.fillRoundRect(tvX+4,tvY+3,tvW-8,tvH-8,4,4);
        gc.setFill(Color.web("#58a6ff",0.15));gc.fillRoundRect(tvX+4,tvY+3,tvW-8,(tvH-8)*0.4,4,4);
        gc.setFill(Color.web("#5c3d2e"));gc.fillRect(tvX+tvW*0.4,tvY+tvH-3,tvW*0.2,3);
        double cX2=oX+18*cs,cY2=oY+9*cs,cW2=cs,cH2=3*cs;
        gc.setFill(Color.web("#6b4226"));gc.fillRoundRect(cX2,cY2,cW2,cH2,5,5);
        gc.setFill(Color.web("#8b5e3c"));gc.fillRoundRect(cX2+3,cY2+3,cW2-6,cH2-6,4,4);
        gc.setStroke(Color.web("#4a2c17"));gc.setLineWidth(1);
        gc.strokeLine(cX2+4,cY2+cH2/3,cX2+cW2-4,cY2+cH2/3);gc.strokeLine(cX2+4,cY2+cH2*2/3,cX2+cW2-4,cY2+cH2*2/3);
        gc.setFill(Color.web("#c8a96e"));gc.fillOval(cX2+cW2/2-3,cY2+cH2/6-3,6,6);gc.fillOval(cX2+cW2/2-3,cY2+cH2/2-3,6,6);gc.fillOval(cX2+cW2/2-3,cY2+cH2*5/6-3,6,6);
    }

    private void drawBedroomFurniture(GraphicsContext gc, double oX, double oY, double cs) {
        double bx=oX+7*cs,by=oY+2*cs,bw=4*cs,bh=3*cs;
        gc.setFill(Color.web("#4a2c1a"));gc.fillRoundRect(bx,by,bw,bh*0.16,4,4);
        gc.setFill(Color.web("#6b8fa8"));gc.fillRect(bx,by+bh*0.16,bw,bh*0.84);
        gc.setFill(Color.web("#8bafc8",0.5));gc.fillRect(bx,by+bh*0.45,bw,bh*0.14);
        gc.setFill(Color.web("#f5f0eb"));gc.fillRoundRect(bx+3,by+bh*0.19,bw/2-5,bh*0.22,3,3);gc.fillRoundRect(bx+bw/2+2,by+bh*0.19,bw/2-5,bh*0.22,3,3);
        gc.setStroke(Color.web("#3a2010",0.5));gc.setLineWidth(1.2);gc.strokeRoundRect(bx,by,bw,bh,4,4);
        gc.setFill(Color.web("#8b6914"));gc.fillRoundRect(oX+6*cs+1,oY+2*cs+1,cs-2,cs-2,3,3);gc.fillRoundRect(oX+12*cs+1,oY+2*cs+1,cs-2,cs-2,3,3);
        gc.setFill(Color.web("#c8a96e"));gc.fillOval(oX+6*cs+cs/2-2,oY+2*cs+cs/2-2,4,4);gc.fillOval(oX+12*cs+cs/2-2,oY+2*cs+cs/2-2,4,4);
        drawWardrobe(gc,oX+2*cs,oY+2*cs,2*cs,4*cs);
        drawWardrobe(gc,oX+16*cs,oY+2*cs,2*cs,4*cs);
        drawDesk(gc,oX+2*cs,oY+9*cs,3*cs,2*cs);
        drawDresser(gc,oX+15*cs,oY+9*cs,3*cs,2*cs);
    }

    private void drawOfficeFurniture(GraphicsContext gc, double oX, double oY, double cs) {
        if(imgPlant!=null){gc.drawImage(imgPlant,oX+cs,oY+cs,cs,cs);gc.drawImage(imgPlant,oX+18*cs,oY+cs,cs,cs);}
        double[][] desks={{2,2,3,2},{2,7,3,2},{6,2,3,2},{6,7,3,2},{12,2,3,2},{12,7,3,2},{16,2,2,2},{16,7,2,2}};
        for(double[] d:desks) drawDesk(gc,oX+d[0]*cs,oY+d[1]*cs,d[2]*cs,d[3]*cs);
        drawFilingCabinet(gc,oX+2*cs,oY+10*cs,3*cs,2*cs);
        drawFilingCabinet(gc,oX+14*cs,oY+10*cs,3*cs,2*cs);
        drawPartition(gc,oX+10*cs,oY+cs,cs,3*cs);
        drawPartition(gc,oX+10*cs,oY+5*cs,cs,5*cs);
    }

    private void drawWardrobe(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#7d5a35"));gc.fillRoundRect(x,y,w,h,6,6);
        gc.setFill(Color.web("#a07840"));gc.fillRoundRect(x+2,y+2,w-4,h-4,4,4);
        gc.setStroke(Color.web("#4a2c10"));gc.setLineWidth(1.5);gc.strokeRoundRect(x,y,w,h,6,6);gc.strokeLine(x+w/2,y,x+w/2,y+h);
        gc.setFill(Color.web("#c8a96e"));gc.fillOval(x+w/2-7,y+h/2-3,5,6);gc.fillOval(x+w/2+2,y+h/2-3,5,6);
        gc.setStroke(Color.web("#4a2c10",0.4));gc.setLineWidth(0.8);
        gc.strokeLine(x+4,y+h*0.25,x+w/2-2,y+h*0.25);gc.strokeLine(x+w/2+2,y+h*0.25,x+w-4,y+h*0.25);
        gc.strokeLine(x+4,y+h*0.7,x+w/2-2,y+h*0.7);gc.strokeLine(x+w/2+2,y+h*0.7,x+w-4,y+h*0.7);
    }

    private void drawDesk(GraphicsContext gc, double x, double y, double w, double h) {
        if(imgWood!=null)gc.drawImage(imgWood,x,y,w,h);
        else{gc.setFill(Color.web("#8b6914"));gc.fillRoundRect(x,y,w,h,4,4);}
        gc.setStroke(Color.web("#5c3d10"));gc.setLineWidth(1.5);gc.strokeRoundRect(x+1,y+1,w-2,h-2,3,3);
        double mx=x+w*0.55,my=y+h*0.12,mw=w*0.35,mh=h*0.55;
        gc.setFill(Color.web("#1c1c1c"));gc.fillRoundRect(mx,my,mw,mh,2,2);
        gc.setFill(Color.web("#58a6ff",0.3));gc.fillRoundRect(mx+1,my+1,mw-2,mh*0.4,1,1);
    }

    private void drawFilingCabinet(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#5c6b7a"));gc.fillRoundRect(x,y,w,h,4,4);
        gc.setStroke(Color.web("#37474f"));gc.setLineWidth(1);gc.strokeRoundRect(x,y,w,h,4,4);
        double dh=(h-6)/3;
        for(int i=0;i<3;i++){double dy=y+3+i*dh;gc.setFill(Color.web("#78909c"));gc.fillRoundRect(x+3,dy,w-6,dh-2,2,2);gc.setFill(Color.web("#b0bec5"));gc.fillRoundRect(x+w/2-6,dy+dh/2-2,12,4,2,2);}
    }

    private void drawDresser(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#8b6914"));gc.fillRoundRect(x,y,w,h,4,4);
        gc.setStroke(Color.web("#5c3d10"));gc.setLineWidth(1);gc.strokeRoundRect(x,y,w,h,4,4);
        double dw=(w-6)/2,dh2=h*0.4;
        for(int i=0;i<2;i++){double dx=x+3+i*dw;gc.setFill(Color.web("#a07840"));gc.fillRoundRect(dx,y+3,dw-2,dh2,2,2);gc.setFill(Color.web("#c8a96e"));gc.fillOval(dx+dw/2-3,y+3+dh2/2-2,6,4);}
        gc.setFill(Color.web("#c8c8c8",0.5));gc.fillRect(x+3,y+3+dh2+3,w-6,h-3-dh2-6);
    }

    private void drawPartition(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#6b4c30"));gc.fillRect(x,y,w,h);
        gc.setStroke(Color.web("#4a2c10"));gc.setLineWidth(1);gc.strokeRect(x,y,w,h);
        int shelves=Math.max(2,(int)(h/(w+0.001)));double shelfH=h/shelves;
        for(int i=1;i<shelves;i++){gc.setStroke(Color.web("#4a2c10"));gc.setLineWidth(1);gc.strokeLine(x,y+i*shelfH,x+w,y+i*shelfH);}
        Color[] bc={Color.web("#da3633"),Color.web("#58a6ff"),Color.web("#3fb950"),Color.web("#d29922"),Color.web("#a371f7"),Color.web("#f0883e")};
        double bkW=Math.max(2,w*0.14);
        for(int shelf=0;shelf<shelves;shelf++){
            double by=y+shelf*shelfH+2,bh=shelfH-4; if(bh<2)continue;
            int cnt=Math.max(1,(int)((w-4)/(bkW+1)));
            for(int b=0;b<cnt;b++){gc.setFill(bc[(shelf*3+b)%bc.length]);gc.fillRect(x+2+b*(bkW+1),by,bkW,bh);}
        }
    }

    private void drawArrowHead(GraphicsContext gc, double x1, double y1, double x2, double y2, Color color) {
        double angle=Math.atan2(y2-y1,x2-x1);double len=10;
        gc.save();gc.setFill(color);gc.translate(x2,y2);gc.rotate(Math.toDegrees(angle));
        gc.beginPath();gc.moveTo(0,0);gc.lineTo(-len,len*0.5);gc.lineTo(-len,-len*0.5);gc.closePath();gc.fill();gc.restore();
    }

    private Button makeRoomNavBtn(String label, String accent, String dimBg) {
        Button btn=new Button(label); btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(navStyle(accent,dimBg,false));
        btn.setOnMouseEntered(e->btn.setStyle(navStyle(accent,dimBg,true)));
        btn.setOnMouseExited(e->btn.setStyle(navStyle(accent,dimBg,false)));
        return btn;
    }

    private String navStyle(String accent, String dimBg, boolean hovered) {
        return "-fx-background-color:"+(hovered?accent:dimBg)+";-fx-border-color:"+accent+";-fx-border-radius:6;-fx-background-radius:6;-fx-border-width:"+(hovered?"2":"1")+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:6 8;-fx-cursor:hand;";
    }

    private void updateRoomNavButtons(RoomType active) {
        if(btnGoSalon==null)return;
        setNavActive(btnGoSalon,   "#3fb950","#0d2b18", active==RoomType.SALON);
        setNavActive(btnGoBedroom, "#a371f7","#1e1033", active==RoomType.BEDROOM);
        setNavActive(btnGoOffice,  "#d29922","#2b2005", active==RoomType.OFFICE);
    }

    private void setNavActive(Button btn, String accent, String dimBg, boolean active) {
        String glow=active?";-fx-effect:dropshadow(gaussian,"+accent+",8,0.4,0,0)":"";
        btn.setStyle("-fx-background-color:"+(active?accent:dimBg)+";-fx-border-color:"+accent+";-fx-border-radius:6;-fx-background-radius:6;-fx-border-width:"+(active?"2":"1")+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:6 8;-fx-cursor:hand"+glow+";");
    }

    private void drawSofa(GraphicsContext gc, double x, double y, double w, double h) {
        boolean vert=h>w; double br=0.28,ar=vert?h*0.10:w*0.10;
        Color body=Color.web("#c8b89a"),back=Color.web("#8b7355"),seat=Color.web("#d8caae"),arm=Color.web("#a89070"),outline=Color.web("#7a6040"),divColor=Color.web("#9a8060");
        gc.setFill(body);gc.fillRoundRect(x,y,w,h,10,10);
        if(!vert){double backH=h*br;gc.setFill(back);gc.fillRoundRect(x,y,w,backH,8,8);gc.fillRect(x,y+backH*0.5,w,backH*0.5);gc.setFill(arm);gc.fillRoundRect(x,y+backH,ar,h-backH,6,6);gc.fillRoundRect(x+w-ar,y+backH,ar,h-backH,6,6);gc.setFill(seat);gc.fillRoundRect(x+ar+2,y+backH+2,w-2*ar-4,h-backH-4,4,4);gc.setStroke(divColor);gc.setLineWidth(1.5);double sw=w-2*ar;for(int i=1;i<3;i++){double dvX=x+ar+sw*i/3;gc.strokeLine(dvX,y+backH+3,dvX,y+h-3);}}
        else{double backW=w*br;gc.setFill(back);gc.fillRoundRect(x,y,backW,h,8,8);gc.fillRect(x+backW*0.5,y,backW*0.5,h);gc.setFill(arm);gc.fillRoundRect(x+backW,y,w-backW,ar,6,6);gc.fillRoundRect(x+backW,y+h-ar,w-backW,ar,6,6);gc.setFill(seat);gc.fillRoundRect(x+backW+2,y+ar+2,w-backW-4,h-2*ar-4,4,4);gc.setStroke(divColor);gc.setLineWidth(1.5);double sh=h-2*ar;for(int i=1;i<3;i++){double dvY=y+ar+sh*i/3;gc.strokeLine(x+backW+3,dvY,x+w-3,dvY);}}
        gc.setStroke(outline);gc.setLineWidth(1.5);gc.strokeRoundRect(x,y,w,h,10,10);
    }

    private void drawCat(GraphicsContext gc, double px, double py, double cs) {
        double cx=px+cs/2,cy=py+cs*0.54,sc=cs/48.0;
        gc.setStroke(Color.web("#d4793a"));gc.setLineWidth(3.2*sc);gc.beginPath();gc.moveTo(cx-7*sc,cy+9*sc);gc.bezierCurveTo(cx-22*sc,cy+20*sc,cx-30*sc,cy+3*sc,cx-21*sc,cy-7*sc);gc.stroke();
        gc.setFill(Color.web("#e8883a"));gc.fillOval(cx-13*sc,cy,26*sc,16*sc);
        gc.setStroke(Color.web("#c06218",0.5));gc.setLineWidth(1.4*sc);gc.strokeLine(cx-5*sc,cy+2*sc,cx-5*sc,cy+14*sc);gc.strokeLine(cx,cy+1*sc,cx,cy+15*sc);gc.strokeLine(cx+5*sc,cy+2*sc,cx+5*sc,cy+14*sc);
        gc.setFill(Color.web("#e8883a"));gc.fillOval(cx-12*sc,cy-15*sc,24*sc,20*sc);
        gc.fillPolygon(new double[]{cx-12*sc,cx-6*sc,cx-17*sc},new double[]{cy-12*sc,cy-24*sc,cy-24*sc},3);gc.fillPolygon(new double[]{cx+12*sc,cx+6*sc,cx+17*sc},new double[]{cy-12*sc,cy-24*sc,cy-24*sc},3);
        gc.setFill(Color.web("#f4a0b0"));gc.fillPolygon(new double[]{cx-11*sc,cx-7*sc,cx-15*sc},new double[]{cy-13*sc,cy-22*sc,cy-22*sc},3);gc.fillPolygon(new double[]{cx+11*sc,cx+7*sc,cx+15*sc},new double[]{cy-13*sc,cy-22*sc,cy-22*sc},3);
        gc.setFill(Color.web("#6dc86d"));gc.fillOval(cx-9*sc,cy-9*sc,7*sc,5.5*sc);gc.fillOval(cx+2*sc,cy-9*sc,7*sc,5.5*sc);
        gc.setFill(Color.web("#111111"));gc.fillOval(cx-6.2*sc,cy-8.8*sc,2.2*sc,4.8*sc);gc.fillOval(cx+4.8*sc,cy-8.8*sc,2.2*sc,4.8*sc);
        gc.setFill(Color.web("#f08090"));gc.fillPolygon(new double[]{cx,cx-2.5*sc,cx+2.5*sc},new double[]{cy-2*sc,cy-5*sc,cy-5*sc},3);
        gc.setStroke(Color.web("#a04040"));gc.setLineWidth(0.8*sc);gc.strokeLine(cx,cy-2*sc,cx-4*sc,cy+1.5*sc);gc.strokeLine(cx,cy-2*sc,cx+4*sc,cy+1.5*sc);
        gc.setStroke(Color.web("#ffffff",0.85));gc.setLineWidth(0.7*sc);
        gc.strokeLine(cx-13*sc,cy-5.5*sc,cx-4*sc,cy-4.5*sc);gc.strokeLine(cx-13*sc,cy-3.5*sc,cx-4*sc,cy-3.5*sc);gc.strokeLine(cx-13*sc,cy-1.5*sc,cx-4*sc,cy-2.5*sc);
        gc.strokeLine(cx+13*sc,cy-5.5*sc,cx+4*sc,cy-4.5*sc);gc.strokeLine(cx+13*sc,cy-3.5*sc,cx+4*sc,cy-3.5*sc);gc.strokeLine(cx+13*sc,cy-1.5*sc,cx+4*sc,cy-2.5*sc);
        gc.setStroke(Color.web("#a05010",0.4));gc.setLineWidth(0.8*sc);gc.strokeOval(cx-13*sc,cy,26*sc,16*sc);gc.strokeOval(cx-12*sc,cy-15*sc,24*sc,20*sc);
    }

    public void setCat(Pet cat) { this.cat = cat; }
    public void startCleaningAnimation(int x,int y,DirtType dirt,int frames){cleaningAnimX=x;cleaningAnimY=y;cleaningAnimDirt=dirt;cleaningAnimFrame=0;cleaningAnimMaxFrames=Math.max(2,frames);cleaningAnimActive=true;}
    public void stopCleaningAnimation(){cleaningAnimActive=false;cleaningAnimFrame=0;}
    public void setReturningStatus(boolean r){this.isReturning=r;}
    public void resetPathHistory(){pathHistory.clear();}
    public void addUserObject(int x,int y,int w,int h,String type){userObjects.add(new PlacedObj(x,y,w,h,type));}
    public void clearUserObjects(){userObjects.clear();}
    public void removeUserObjectAt(int x,int y){userObjects.removeIf(obj->x>=obj.x&&x<obj.x+obj.w&&y>=obj.y&&y<obj.y+obj.h);}

    public Canvas      getCanvas()             {return canvas;}
    public Button      getBtnAddDirt()         {return btnAddDirt;}
    public Button      getBtnAddObstacle()     {return btnAddObstacle;}
    public Button      getBtnRemoveFurniture() {return btnRemoveFurniture;}
    public Button      getBtnStart()           {return btnStart;}
    public Button      getBtnPause()           {return btnPause;}
    public Button      getBtnReset()           {return btnReset;}
    public Button      getBtnReturn()          {return btnReturn;}
    public RadioButton getRbDust()             {return rbDust;}
    public RadioButton getRbLiquid()           {return rbLiquid;}
    public RadioButton getRbStain()            {return rbStain;}
    public RadioButton getRbWall()             {return rbWall;}
    public RadioButton getRbSofa()             {return rbSofa;}
    public RadioButton getRbTable()            {return rbTable;}
    public Slider      getSliderSpeed()        {return sliderSpeed;}
    public Slider      getSliderBattery()      {return sliderBattery;}
    public RadioButton getRbRandom()           {return rbRandom;}
    public RadioButton getRbSpiral()           {return rbSpiral;}
    public RadioButton getRbWallFollow()       {return rbWallFollow;}

    public void updateStats(int cleaned,int totalArea,int unreachable,int dirtyCount,String time){
        lblTotalArea.setText(totalArea+" m2");
        int pct=totalArea>0?(int)((cleaned*100.0f)/totalArea):0;
        lblCleanedArea.setText(String.format("%d m2 (%%%d)",cleaned,pct));
        lblDirtyArea.setText(dirtyCount+" hucre");
        lblUnreachable.setText(unreachable+" m2");
        lblTime.setText(time);
    }
}
