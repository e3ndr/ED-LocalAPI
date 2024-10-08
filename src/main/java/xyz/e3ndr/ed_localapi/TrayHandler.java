package xyz.e3ndr.ed_localapi;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import lombok.NonNull;

public class TrayHandler {
    private static final Image IMAGE;
    static {
        Image i;
        try {
            i = new ImageIcon(TrayHandler.class.getResource("/icon.png")).getImage();
        } catch (NullPointerException e) {
            i = new ImageIcon(TrayHandler.class.getResource("/resources/icon.png")).getImage(); // Probably Eclipse mangling resources. Try again.
        }
        IMAGE = i;
    }

    private static SystemTray tray;
    private static TrayIcon icon;

    public static void tryCreateTray() {
        if (tray == null) {
            // Check the SystemTray support
            if (!SystemTray.isSupported()) {
                throw new IllegalStateException("Cannot add TrayIcon.");
            }

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            tray = SystemTray.getSystemTray();
            PopupMenu popup = new PopupMenu();

            // Create the popup menu components
            MenuItem itemExit = new MenuItem("Exit");

//            currentScreenText.setEnabled(false);

            // Add components to popup menu
            popup.add(itemExit);

            itemExit.addActionListener((ActionEvent e) -> {
                destroy();
                System.exit(0);
            });

            // Setup the tray icon.
            icon = new TrayIcon(IMAGE);

            icon.setToolTip("EDLA");
            icon.setImageAutoSize(true);
            icon.setPopupMenu(popup);

            try {
                tray.add(icon);
            } catch (AWTException e) {}

            // Remove the icon on shutdown.
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        tray.remove(icon);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            throw new IllegalStateException("Tray handler is already initialized.");
        }
    }

    public static void destroy() {
        if (tray != null) {
            tray.remove(icon);
        }
    }

    public static void notify(@NonNull String message, @NonNull MessageType type) {
        if (icon == null) {
            throw new IllegalStateException();
        }

        icon.displayMessage("EDLA", message, type);
    }

}
