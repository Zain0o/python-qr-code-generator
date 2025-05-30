module CWGUI {
    requires javafx.controls;
    requires javafx.graphics;
    requires transitive javafx.base;
    requires org.json;

    exports GUI;
    exports RobotSim;
}
