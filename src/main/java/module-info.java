module com.runescape.runescapeflippingutility {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires org.json;
    requires java.desktop;

    opens com.runescape to javafx.fxml;
    exports com.runescape;
}

