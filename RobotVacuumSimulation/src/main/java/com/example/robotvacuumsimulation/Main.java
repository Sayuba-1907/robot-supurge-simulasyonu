package com.example.robotvacuumsimulation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Robot Süpürge Simülasyonu");
        primaryStage.setResizable(false);

        RoomSelectionScreen selectionScreen = new RoomSelectionScreen(
                selectedRoom -> startSimulation(primaryStage, selectedRoom)
        );

        Scene selectionScene = new Scene(selectionScreen, 950, 620);
        primaryStage.setScene(selectionScene);
        primaryStage.show();
    }

    private void startSimulation(Stage stage, RoomType selectedRoom) {
        Room           salonRoom  = new Room(20, 14);
        Robot          robot      = new Robot(11, 7);
        SimulationView view       = new SimulationView(20, 14);
        SimulationController controller = new SimulationController(robot, salonRoom, view, selectedRoom);

        controller.startSimulation();

        Scene simScene = new Scene(view, 1150, 750);
        stage.setTitle("Robot Süpürge Simülasyonu — " + selectedRoom.displayName);
        stage.setResizable(true);
        stage.setScene(simScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
