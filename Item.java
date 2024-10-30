package com.runescape;

import javafx.beans.property.*;
import java.math.BigInteger;
import java.util.Optional;

public class Item {
    private final SimpleStringProperty itemName;
    private final SimpleIntegerProperty itemID;
    private final SimpleIntegerProperty volume;
    private final ObjectProperty<Integer> tradeLimit;
    private final ObjectProperty<BigInteger> buyPrice;
    private final ObjectProperty<BigInteger> sellPrice;
    private final ObjectProperty<BigInteger> hourAveBuyPrice;
    private final ObjectProperty<BigInteger> hourAveSellPrice;
    private final ObjectProperty<BigInteger> hourAveProfit;
    private final ObjectProperty<BigInteger> profit;
    private final BooleanProperty stable;
    private final BooleanProperty members;
    private final ObjectProperty<BigInteger> ROI;
    private final ObjectProperty<BigInteger> volumeProfitPotential;
    private final ObjectProperty<BigInteger> hourAveSellVolume;



    public Item(String itemName, int itemID, int volume, int tradeLimit,
                BigInteger buyPrice, BigInteger sellPrice, BigInteger hourAveBuy,
                BigInteger hourAveSell, BigInteger hourAveProfit, boolean members, BigInteger ROI, BigInteger volumeProfitPotential, BigInteger hourAveSellVolume) {
        this.itemName = new SimpleStringProperty(itemName);
        this.itemID = new SimpleIntegerProperty(itemID);
        this.volume = new SimpleIntegerProperty(volume);
        this.tradeLimit = new SimpleObjectProperty<>(tradeLimit);
        this.buyPrice = new SimpleObjectProperty<>(buyPrice);// initialize to 0
        this.sellPrice = new SimpleObjectProperty<>(sellPrice); //initialize to 0
        this.hourAveBuyPrice = new SimpleObjectProperty<>(hourAveBuy);
        this.hourAveSellPrice = new SimpleObjectProperty<>(hourAveSell);
        this.hourAveProfit = new SimpleObjectProperty<>(hourAveProfit);
        this.members = new SimpleBooleanProperty(members);
        this.ROI = new SimpleObjectProperty<>(ROI);
        this.volumeProfitPotential = new SimpleObjectProperty<>(volumeProfitPotential);
        this.hourAveSellVolume = new SimpleObjectProperty<>(hourAveSellVolume);

        // Calculate profit and stability
        this.profit = new SimpleObjectProperty<>(BigInteger.ZERO);
        this.stable = new SimpleBooleanProperty(isStable());

        // Add listeners to update profit and stability when prices change
        this.buyPrice.addListener((obs, oldPrice, newPrice) -> {
            setProfit();
            setStable();
            setVolumeProfitPotential();
        });
        this.sellPrice.addListener((obs, oldPrice, newPrice) -> {
            setProfit();
            setStable();
            setVolumeProfitPotential();
        });

    }


    // Getters for JavaFX properties
    public StringProperty itemNameProperty() {
        return itemName;
    }

    public IntegerProperty itemIDProperty() {
        return itemID;
    }

    public IntegerProperty volumeProperty() {
        return volume;
    }

    public ObjectProperty<Integer> tradeLimitProperty() {
        return tradeLimit;
    }

    public ObjectProperty<BigInteger> buyPriceProperty() {
        return buyPrice;
    }

    public ObjectProperty<BigInteger> sellPriceProperty() {
        return sellPrice;
    }

    public ObjectProperty<BigInteger> hourAveBuyPriceProperty() {
        return hourAveBuyPrice;
    }

    public ObjectProperty<BigInteger> hourAveSellPriceProperty() {
        return hourAveSellPrice;
    }

    public ObjectProperty<BigInteger> profitProperty() {
        return profit;
    }

    public BooleanProperty membersProperty() {
        return members;
    }

    public BooleanProperty stableProperty() {
        return stable;
    }

    public ObjectProperty<BigInteger> ROIProperty(){
        return ROI;
    }

    // Utility methods for non-property access
    public String getItemName() {
        return itemName.get();
    }

    public int getItemID() {
        return itemID.get();
    }

    public int getVolume() {
        return volume.get();
    }

    public BigInteger getHourVolume() {
        return hourAveSellVolume.get();
    }

    public int getTradeLimit() {
        return tradeLimit.get();
    }

    public BigInteger getBuyPrice() {
        return buyPrice.get();
    }

    public BigInteger getSellPrice() {
        return sellPrice.get();
    }

    public BigInteger getHourAveBuyPrice() {
        return hourAveBuyPrice.get();
    }

    public BigInteger getHourAveSellPrice() {
        return hourAveSellPrice.get();
    }

    public BigInteger getProfit() {
        return profit.get();
    }

    public boolean isMembers() {
        return members.get();
    }

    public boolean isStable() {
        // Fetch the average profit and current profit once
        BigInteger averageProfit = getAveProfit();
        BigInteger currentProfit = getProfit();

        // Calculate the lower (98%) and upper (103%) bounds
        BigInteger lowerBound = averageProfit.multiply(BigInteger.valueOf(98)).divide(BigInteger.valueOf(100));
        BigInteger upperBound = averageProfit.multiply(BigInteger.valueOf(103)).divide(BigInteger.valueOf(100));

        // Check if the current profit is within the ±5% range
        return currentProfit.compareTo(lowerBound) >= 0 && currentProfit.compareTo(upperBound) <= 0;
    }


    // Setters for the fields
    public void setItemName(String itemName) {
        this.itemName.set(itemName);
    }

    public void setItemID(int itemID) {
        this.itemID.set(itemID);
    }

    public void setVolume(int volume) {
        this.volume.set(volume);
    }

    public void setVolumeProfitPotential () {
        setHourAveSellVolume(hourAveSellVolume.get());
        BigInteger volPotential;
        BigInteger tradeLim = BigInteger.valueOf(tradeLimit.get());
        if (hourAveSellVolume.get().compareTo(tradeLim) >= 0 && profit.get().compareTo(BigInteger.ONE) > 0) {
            volPotential = profit.get().multiply(tradeLim);
        } else  if (profit.get().compareTo(BigInteger.ONE) > 0){
            volPotential = profit.get().multiply(hourAveSellVolume.get());
        } else {
            volPotential = BigInteger.ZERO;
        }
        this.volumeProfitPotential.set(volPotential);
    }

    public BigInteger getVolumeProfitPotential(){
        return this.volumeProfitPotential.get();
    }

    public void setHourAveSellVolume(BigInteger aveHourVol) {
        //6hr ave / 6 = 1h ave
        BigInteger aveHourVolAdjusted = aveHourVol.divide(BigInteger.valueOf(6));
        this.hourAveSellVolume.set(aveHourVolAdjusted);
    }

    public void setTradeLimit(int tradeLimit) {
        this.tradeLimit.set(tradeLimit);
    }

    public void setBuyPrice(BigInteger buyPrice) {
        System.out.println("Setting Buy Price: " + buyPrice);
        this.buyPrice.set(buyPrice);
    }

    public void setSellPrice(BigInteger sellPrice) {
        System.out.println("Setting Sell Price: " + sellPrice);
        this.sellPrice.set(sellPrice);
    }

    public void setHourAveBuyPrice(BigInteger hourAveBuyPrice) {
        this.hourAveBuyPrice.set(hourAveBuyPrice);
    }

    public void setHourAveSellPrice(BigInteger hourAveSellPrice) {
        this.hourAveSellPrice.set(hourAveSellPrice);
    }

    public void setProfit() {
        this.profit.set(calculateProfit());
    }

    public void setAveProfit(){
        this.hourAveProfit.set(calculateAveProfit());
    }

    public BigInteger getAveProfit() {
        return hourAveProfit.get();
    }

    public BigInteger calculateAveProfit() {
        BigInteger tax = calculateTax(hourAveSellPrice.get());
        return hourAveSellPrice.get().subtract(hourAveBuyPrice.get()).subtract(tax);
    }

    public void setMembers(boolean members) {
        this.members.set(members);
    }

    public void setStable() {
        this.stable.set(isStable());
    }

    // Helper method to calculate profit
    public BigInteger calculateProfit() {
        BigInteger tax = calculateTax(sellPrice.get());
        return sellPrice.get().subtract(buyPrice.get()).subtract(tax);
    }

    // Helper method to calculate tax
    private BigInteger calculateTax(BigInteger price) {
        if (price.compareTo(BigInteger.valueOf(100)) > 0) {
            BigInteger tax = price.multiply(BigInteger.valueOf(1)).divide(BigInteger.valueOf(100));
            return tax.min(BigInteger.valueOf(5000000)); // Cap at 5 million coins
        }
        return BigInteger.ZERO;
    }

    private BigInteger calculateROI() {
        if (buyPrice.get().compareTo(BigInteger.ZERO) > 0) {
            // ROI = (Profit / BuyPrice) * 100
            return profit.get().multiply(BigInteger.valueOf(100)).divide(buyPrice.get());
        } else {
            return BigInteger.ZERO; // Avoid division by zero
        }
    }

    public void setROI() {
        this.ROI.set(calculateROI());
    }
}