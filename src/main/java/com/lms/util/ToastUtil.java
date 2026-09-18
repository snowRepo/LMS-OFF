package com.lms.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Utility for displaying floating "Toast" notifications within the application window.
 */
public class ToastUtil {

    public static void show(String message) {
        Window window = Navigator.getStage();
        if (window == null) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        Label label = new Label(message);
        // Grey background, dark text, with a subtle primary color left border
        label.setStyle("-fx-background-color: #f4f5f7; -fx-text-fill: #333333; -fx-padding: 12px 20px; " +
                       "-fx-border-color: -fx-default-button; -fx-border-width: 0 0 0 4; -fx-font-size: 14px; " +
                       "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        StackPane pane = new StackPane(label);
        pane.setPadding(new Insets(10));
        popup.getContent().add(pane);

        // Position at the top right of the window
        popup.setOnShown(e -> {
            popup.setX(window.getX() + window.getWidth() - popup.getWidth() - 20);
            popup.setY(window.getY() + 40);
        });

        popup.show(window);

        // Fade out animation
        Timeline timeline = new Timeline();
        KeyFrame kf1 = new KeyFrame(Duration.millis(3500), new KeyValue(pane.opacityProperty(), 1.0));
        KeyFrame kf2 = new KeyFrame(Duration.millis(4000), new KeyValue(pane.opacityProperty(), 0.0));
        timeline.getKeyFrames().addAll(kf1, kf2);
        timeline.setOnFinished(e -> popup.hide());
        timeline.play();
    }
}
