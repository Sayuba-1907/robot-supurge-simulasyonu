module com.example.robotvacuumsimulation {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;

    opens com.example.robotvacuumsimulation to javafx.fxml;
    exports com.example.robotvacuumsimulation;
}