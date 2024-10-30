package com.runescape;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class FlipperController {

    @FXML
    private VBox MainMenu;

    @FXML
    protected void onByVolumeClick() {
        loadPage("ByVolume.fxml");
    }

    @FXML
    protected void onByProfitClick() {
        loadPage("ByProfit.fxml");
    }

    @FXML
    protected void onSearchClick() {
        loadPage("SearchItem.fxml");
    }

    // Method to get the current Stage
    protected Stage getCurrentStage() {
        return (Stage) MainMenu.getScene().getWindow(); // You can replace mainMenuButton with any UI component present in the scene
    }

    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = getCurrentStage(); // Get the current stage
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load FXML file: " + fxmlFile);
        }
    }
}
