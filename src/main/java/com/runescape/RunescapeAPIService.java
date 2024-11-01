package com.runescape;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RunescapeAPIService {

    // HTTP client
    private final OkHttpClient client = new OkHttpClient();

    // API endpoints
    private final String bulkAPI = "https://chisel.weirdgloop.org/gazproj/gazbot/os_dump.json";
    private final String latestAPI = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private final String oneHourAPI = "https://prices.runescape.wiki/api/v1/osrs/1h";
    private final String sixHourAPI = "https://prices.runescape.wiki/api/v1/osrs/6h";
    private static final List<Item> items = Collections.synchronizedList(new ArrayList<>());


    // Fetch data from all APIs simultaneously
    public void fetchData() {

        CompletableFuture<Void> bulkDataFuture = CompletableFuture.runAsync(this::fetchBulkData);

        // Chain latest data to run after bulk data completes
        CompletableFuture<Void> latestDataFuture = bulkDataFuture.thenRunAsync(this::fetchLatestData);

        // Chain one hour data to run after latest data completes
        CompletableFuture<Void> oneHourDataFuture = bulkDataFuture.thenRunAsync(this::fetchSixHourData);


        // Wait for all futures to complete
        oneHourDataFuture.thenRun(() -> {
            System.out.println("All API calls completed in order: Bulk -> Latest -> OneHour.");
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }




    private void fetchBulkData() {
        fetchDataFromAPI(bulkAPI, this::processBulkData);
        System.out.println("Total items fetched after all API calls: " + items.size());

    }

    // Make the API call for the latest data
    private void fetchLatestData() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(latestAPI).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("Failed to fetch latest data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    System.out.println("Unexpected code: " + response);
                    return;
                }

                // Log the raw response
                String jsonResponse = response.body().string();
                processLatestData(jsonResponse);
            }
        });
    }

    private void fetchSixHourData() {
        fetchDataFromAPI(sixHourAPI, this::processSixHourData);
    }

    // API Fetch method for different endpoints
    private void fetchDataFromAPI(String url, Consumer<String> responseProcessor) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    responseProcessor.accept(jsonResponse);
                } else {
                    System.out.println("Failed to fetch data: " + response.message());
                }
            }
        });
    }
    private void processBulkData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            for (String key : jsonObject.keySet()) {
                try {
                    int itemId = Integer.parseInt(key); // Parse itemID
                    JSONObject itemData = jsonObject.getJSONObject(key);
                    String itemName = itemData.getString("name");
                    boolean isMembers = itemData.getBoolean("members");

                    // Handle the 'limit' and 'volume' fields safely
                    int tradeLimit = itemData.has("limit") ? itemData.getInt("limit") : 0;
                    int volume = itemData.has("volume") ? itemData.getInt("volume") : 0;

                    // Check if item already exists in the list
                    Optional<Item> existingItem = items.stream()
                            .filter(item -> item.getItemID() == itemId)
                            .findFirst();

                    if (existingItem.isPresent()) {
                        // Update existing item
                        Item item = existingItem.get();
                        item.setVolume(volume);
                        item.setTradeLimit(tradeLimit);
                        item.setMembers(isMembers);
                        System.out.printf("Updated existing item: %s (ID: %d)%n", itemName, itemId);
                    } else {
                        // Create new item if it doesn't exist
                        Item newItem = new Item(itemName, itemId, volume, tradeLimit, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, isMembers, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
                        items.add(newItem);
                        System.out.printf("Added new item: %s (ID: %d)%n", itemName, itemId);
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Invalid item ID found: " + key + ". Skipping this entry.");
                }
            }
        } catch (JSONException e) {
            System.out.println("JSON Exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("General Exception: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Total items processed from bulk data: " + items.size());
    }


    private void processLatestData(String jsonResponse) {
        try {
            System.out.println("\n\n------------------   Processing Latest Data...   -------------------\n\n");

            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject dataJson = jsonObject.getJSONObject("data");

            // Check if the data is empty
            if (dataJson.isEmpty()) {
                System.out.println("No data found in the latest API response.");
                return;
            }

            // synchonize manually to avoid collision updates to the ArrayList
            synchronized (items) {
                // Update items with buyPrice and sellPrice
                for (Item item : items) {
                    String itemIdStr = String.valueOf(item.getItemID());
                    System.out.println("Checking item with ID: " + itemIdStr); // Log the item ID being checked

                    if (dataJson.has(itemIdStr)) {
                        JSONObject prices = dataJson.getJSONObject(itemIdStr);

                        // Log the prices data for the current item
                        System.out.println("Prices data for item ID " + itemIdStr + ": " + prices);

                        // Check for 'high' and 'low' prices
                        if (prices.has("high") && prices.has("low")) {
                            int buyPrice = prices.getInt("low");
                            int sellPrice = prices.getInt("high");

                            // Log the values before setting them
                            System.out.println("Fetched Buy Price: " + buyPrice);
                            System.out.println("Fetched Sell Price: " + sellPrice);
                            System.out.println("Fetched Sell Volume: " + sellPrice);

                            // Set prices
                            item.setBuyPrice(BigInteger.valueOf(buyPrice));
                            item.setSellPrice(BigInteger.valueOf(sellPrice));
                            item.setStable(); // set stability after average and current prices are available
                            item.setROI();

                            // Log successful setting of variables
                            System.out.println("Set Buy Price: " + item.getBuyPrice());
                            System.out.println("SetSell Price: " + item.getSellPrice());

                            // Log the updated item state
                            System.out.printf("Item ID %d updated: Buy Price = %d, Sell Price = %d%n",
                                    item.getItemID(), item.getBuyPrice(), item.getSellPrice());
                        } else {
                            System.out.printf("Item ID %s: Missing 'high' or 'low' prices in latest data.%n", itemIdStr);
                            // Log the missing fields
                            System.out.println("Prices JSON: " + prices.toString());
                        }
                    } else {
                        System.out.println("Item ID " + item.getItemID() + " not found in latest data.");
                    }
                }
            }
            System.out.println("Prices updated for items.");
        } catch (JSONException e) {
            System.out.println("JSON Exception: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        } catch (Exception e) {
            System.out.println("General Exception: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }
    }

    private void processSixHourData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject dataJson = jsonObject.getJSONObject("data");

            // Log the one-hour API response
            System.out.println("Processing Hour Data ...");

            for (String key : dataJson.keySet()) {
                int itemId = Integer.parseInt(key);
                JSONObject itemData = dataJson.getJSONObject(key);

                int avgHighPrice = itemData.optInt("avgHighPrice", 0);
                int avgLowPrice = itemData.optInt("avgLowPrice", 0);
                BigInteger aveSellVolume = BigInteger.valueOf(itemData.getLong("highPriceVolume"));
                // Log average prices
                System.out.printf("Item ID %d: Avg High Price = %d, Avg Low Price = %d%n", itemId, avgHighPrice, avgLowPrice);

                synchronized (items) {
                    for (Item item : items) {
                        if (item.getItemID() == itemId) {
                            item.setHourAveSellPrice(BigInteger.valueOf(avgHighPrice));
                            item.setHourAveBuyPrice(BigInteger.valueOf(avgLowPrice));
                            item.setAveProfit();
                            item.setHourAveSellVolume(aveSellVolume);
                            item.setVolumeProfitPotential();

                            System.out.println("Ave. priced updated.");
                        }
                    }
                }
            }
                System.out.println("One Hour prices processed.");
            } catch(JSONException e){
                System.out.println("JSON Exception: " + e.getMessage());
            } catch(Exception e){
                System.out.println("General Exception: " + e.getMessage());
            }

    }


    // Getter for items
    public static List<Item> getItems() {
        return items; // Return the existing list of items
    }
}
