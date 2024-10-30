package com.runescape;

// FXML Formatting
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.util.*;

import javafx.scene.control.TableColumn;

// Others
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class VolumeController implements Initializable {

    @FXML
    public Button refreshButton;

    // Store the current spinner value
    private BigInteger currentSpinnerValue; // Initialize with a default value
    private BigInteger min = BigInteger.ONE;
    private BigInteger max = new BigInteger("5000000000"); // 5 billion initial max
    private RunescapeAPIService apiService;

    @FXML
    private TableView<Item> tableViewVol;
    @FXML
    private TableColumn<Item, String> nameColumn;
    @FXML
    private TableColumn<Item, Integer> buyPriceColumn;
    @FXML
    private TableColumn<Item, Integer> sellPriceColumn;
    @FXML
    private TableColumn<Item, Integer> tradeLimitColumn;
    @FXML
    private TableColumn<Item, Integer> volumeColumn;
    @FXML
    private TableColumn<Item, BigInteger> profitPotentialColumn; // Adjusted to use BigInteger if needed
    @FXML
    private TableColumn<Item, Integer> ROIColumn;

    @FXML
    private Button mainMenuButton; // Button reference
    @FXML
    private Spinner<BigInteger> maxPrice;

    @FXML
    private ToggleButton toggleMembers; // Members toggle button
    private boolean showMembersItems = false;

    @FXML
    private ToggleButton toggleStable;
    private boolean stableOnlyView;

    @FXML
    private MenuButton stepSizeMenu;
    @FXML
    private int stepSize; // Default step size of 1,000gp

    //NOT USED!!!!!!!!!!!!!!!
    public void startDataRefresh() {
        long delay = 5000; // Delay (5 seconds)
        Timer timer = new Timer();
        // Create a TimerTask
        TimerTask refresh = new TimerTask() {
            @Override
            public void run() {
                apiService.fetchData();// refresh data
                loadDataWithSpinnerFilter();
            }
        };
        timer.schedule(refresh, 0, delay);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        // Bind each column to the corresponding property in the Item class
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        buyPriceColumn.setCellValueFactory(new PropertyValueFactory<>("buyPrice"));
        sellPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        tradeLimitColumn.setCellValueFactory(new PropertyValueFactory<>("tradeLimit"));
        volumeColumn.setCellValueFactory(new PropertyValueFactory<>("volume"));
        profitPotentialColumn.setCellValueFactory(new PropertyValueFactory<>("volumeProfitPotential"));
        ROIColumn.setCellValueFactory(new PropertyValueFactory<>("ROI"));

        //call APIs and populate list
        apiService.fetchData();

        // Load data into the TableView
        loadData();

        // Setup spinner and its listeners
        setupSpinner();

        // Setup stepSize menu items
        setupStepSizeMenu();

        loadDataWithSpinnerFilter();


    }

    private void setupStepSizeMenu() {
        for (MenuItem menuitem: stepSizeMenu.getItems()) {
            menuitem.setOnAction(actionEvent -> {
                //get stepSize from menu item text
                stepSize = Integer.parseInt(menuitem.getText());
                stepSizeMenu.setText(menuitem.getText()); //updates to show the selected size
            });
        }
    }

    private void loadData() {
        CompletableFuture.runAsync(() -> {
            List<Item> items = RunescapeAPIService.getItems();
            System.out.println("Items fetched: " + items.size()); // Debugging line
            for (Item item : items) {
                System.out.println("Item: " + item.getItemName() + ", Buy Price: " + item.getBuyPrice() + ", Sell Price: " + item.getSellPrice()); // Debugging line
            }
            Platform.runLater(() -> {
                tableViewVol.getItems().setAll(items);
                tableViewVol.refresh(); // Refresh the TableView
            });
        });
    }


    public VolumeController() {
        this.apiService = new RunescapeAPIService();
        this.apiService.fetchData(); // Ensure data is fetched upon controller initialization
    }

    private void setupSpinner() {
        BigInteger initialValue = max;
        BigInteger step = BigInteger.valueOf(stepSize);
        stepSize = 1000;// step size of 1,000

        // Set default text for MenuButton
        stepSizeMenu.setText(String.valueOf(stepSize));

        SpinnerValueFactory<BigInteger> valueFactory = new SpinnerValueFactory<BigInteger>() {
            {
                setValue(initialValue); // Set initial value
            }

            @Override
            public void decrement(int steps) {
                // Use the dynamically set stepSize from the stepSizeMenu
                BigInteger step = BigInteger.valueOf(stepSize); // Fetch latest stepSize
                BigInteger newValue = getValue().subtract(step.multiply(BigInteger.valueOf(steps)));
                if (newValue.compareTo(min) >= 0) {
                    setValue(newValue);
                } else {
                    setValue(min);
                }
            }

            @Override
            public void increment(int steps) {
                // Use the dynamically set stepSize from the stepSizeMenu
                BigInteger step = BigInteger.valueOf(stepSize); // Fetch latest stepSize
                BigInteger newValue = getValue().add(step.multiply(BigInteger.valueOf(steps)));
                if (newValue.compareTo(max) <= 0) {
                    setValue(newValue);
                } else {
                    setValue(max);
                }
            }
        };

        maxPrice.setValueFactory(valueFactory);

        // Validate input when focus is lost
        maxPrice.getEditor().focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) { // Focus lost, validate input
                validateSpinnerInput(maxPrice, min, max);
            }
        });

        // Validate input when keys are pressed
        maxPrice.getEditor().setOnAction(event -> {
            validateSpinnerInput(maxPrice, min, max);
        });

        // Add listener to update currentSpinnerValue without refreshing the table
        maxPrice.getEditor().setOnAction(event -> {
            validateSpinnerInput(maxPrice, min, max);
            currentSpinnerValue = maxPrice.getValue(); // Update currentSpinnerValue
        });
    }

    // Helper method to validate the spinner input
    private void validateSpinnerInput(Spinner<BigInteger> spinner, BigInteger min, BigInteger max) {
        String newValue = spinner.getEditor().getText();
        try {
            BigInteger enteredValue = new BigInteger(newValue);
            if (enteredValue.compareTo(min) >= 0 && enteredValue.compareTo(max) <= 0) {
                spinner.getValueFactory().setValue(enteredValue);
            } else {
                spinner.getEditor().setText(spinner.getValue().toString()); // Revert to previous value if out of range
            }
        } catch (NumberFormatException e) {
            spinner.getEditor().setText(spinner.getValue().toString()); // Revert to previous value if invalid
        }
    }

    @FXML
    protected void onMembersToggleClick() {
        // If toggled, show members items
        showMembersItems = toggleMembers.isSelected();
    }

    @FXML
    protected void onStableToggleClick() {
        // If toggled, show items with stable 6h average
        stableOnlyView = toggleStable.isSelected();
    }

    @FXML
    protected void onMainMenuClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/runescape/MainMenu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainMenuButton.getScene().getWindow(); // Get the window from the button
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.out.println("Failed to load FXML file: MainMenu.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    protected void onRefreshButtonClick() {
        // Fetch and refresh table data when refresh button is clicked
        currentSpinnerValue = maxPrice.getValue();
        loadDataWithSpinnerFilter();
        apiService.fetchData();//update data

    }


    private void loadDataWithSpinnerFilter() {
        CompletableFuture.runAsync(() -> {
            List<Item> allItems = RunescapeAPIService.getItems();
            apiService.fetchData(); //update data

            List<Item> filteredItems = allItems.stream()
                    .filter(item -> item.getBuyPrice().compareTo(currentSpinnerValue) <= 0) // Max buy price filter
                    .filter(item -> item.getVolumeProfitPotential().compareTo(BigInteger.ZERO) > 0) // Exclude items with negative profit
                    .filter(item -> !stableOnlyView || (item.isStable() && (showMembersItems || !item.isMembers()))) // Toggle to show stable items, respecting member filter
                    .filter(item -> showMembersItems || !item.isMembers()) // Always show F2P items, toggle members button to show members
                    .sorted((item1, item2) -> item2.getVolumeProfitPotential().compareTo(item1.getVolumeProfitPotential()))
                    .toList();

            Platform.runLater(() -> {
                ObservableList<Item> observableItems = FXCollections.observableArrayList(filteredItems);
                tableViewVol.setItems(observableItems);
                tableViewVol.refresh();
            });
        });
    }


}
