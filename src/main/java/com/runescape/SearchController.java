package com.runescape;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class SearchController {

    // TableView parameters
    @FXML
    private TableView<Item> tableViewSearch;
    @FXML
    private TableColumn<Item, String> nameColumn;
    @FXML
    private TableColumn<Item, String> membersColumn;
    @FXML
    private TableColumn<Item, Integer> buyPriceColumn;
    @FXML
    private TableColumn<Item, Integer> sellPriceColumn;
    @FXML
    private TableColumn<Item, Integer> tradeLimitColumn;
    @FXML
    private TableColumn<Item, Integer> volumeColumn;
    @FXML
    private TableColumn<Item, String> profitColumn;
    @FXML
    private TableColumn<Item, BigInteger> profitPotentialColumn; // Adjusted to use BigInteger if needed
    @FXML
    private TableColumn<Item, Integer> ROIColumn;

    @FXML
    private TextField searchBar;
    @FXML
    private Button mainMenuButton;

    private RunescapeAPIService apiService;

    @FXML
    protected void onMainMenuClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/runescape/MainMenu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainMenuButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.out.println("Failed to load FXML file: MainMenu.fxml");
            e.printStackTrace();
        }
    }

    public void initialize(URL arg0, ResourceBundle arg1) {
        // Bind each column to the corresponding property in the Item class
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        membersColumn.setCellValueFactory(new PropertyValueFactory<>("members"));
        buyPriceColumn.setCellValueFactory(new PropertyValueFactory<>("buyPrice"));
        sellPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        tradeLimitColumn.setCellValueFactory(new PropertyValueFactory<>("tradeLimit"));
        volumeColumn.setCellValueFactory(new PropertyValueFactory<>("volume"));
        profitPotentialColumn.setCellValueFactory(new PropertyValueFactory<>("volumeProfitPotential"));
        profitColumn.setCellValueFactory(new PropertyValueFactory<>("profit"));
        ROIColumn.setCellValueFactory(new PropertyValueFactory<>("ROI"));

        apiService.fetchData();
        loadData();

        // Listen for text changes in the search bar and apply filter
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> loadDataWithSearchFilter(newValue.toLowerCase()));
    }

    private void loadData() {
        CompletableFuture.runAsync(() -> {
            List<Item> items = RunescapeAPIService.getItems();
            Platform.runLater(() -> {
                tableViewSearch.getItems().setAll(items);
                tableViewSearch.refresh();
            });
        });
    }

    public SearchController() {
        this.apiService = new RunescapeAPIService();
        this.apiService.fetchData();
    }

    private void loadDataWithSearchFilter(String filterText) {
        CompletableFuture.runAsync(() -> {
            List<Item> allItems = RunescapeAPIService.getItems();
            apiService.fetchData(); // Update data

            List<Item> filteredItems = allItems.stream()
                    .filter(item -> item.getItemName().toLowerCase().contains(filterText))
                    .sorted((item1, item2) -> item1.getItemName().compareTo(item2.getItemName()))
                    .toList();

            Platform.runLater(() -> {
                ObservableList<Item> observableItems = FXCollections.observableArrayList(filteredItems);
                tableViewSearch.setItems(observableItems);
                tableViewSearch.refresh();
            });
        });
    }
}
