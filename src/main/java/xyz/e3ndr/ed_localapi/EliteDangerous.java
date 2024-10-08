package xyz.e3ndr.ed_localapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.SneakyThrows;
import xyz.e3ndr.ed_localapi.util.FileWatcher;
import xyz.e3ndr.ed_localapi.util.ProcessWatcher;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class EliteDangerous {
    private static final FastLogger LOGGER = new FastLogger();
    private static final Object LOCK = new Object();

    public static final File GAME_DIR = new File(System.getProperty("user.home"), "Saved Games\\Frontier Developments\\Elite Dangerous");
    public static volatile boolean isGameRunning = false;
    public static volatile @Nullable File currentJournal = null;

    private static List<Thread> fileWatchers = new LinkedList<>();
    private static Map<String, List<ContentListener>> fileListeners = new HashMap<>();

    private static List<GameListener> gameListeners = new LinkedList<>();

    static {
        LOGGER.info("Game dir: %s", GAME_DIR);
    }

    private static ProcessWatcher process = new ProcessWatcher("EliteDangerous64.exe") {
        @SneakyThrows
        @Override
        public void onChange(boolean currentState) {
            synchronized (LOCK) {
                if (currentState) {
                    LOGGER.info("Game has been opened! Waiting for a bit and then starting all file watchers...");
                    TimeUnit.SECONDS.sleep(20); // Wait a bit for the game to load.

                    for (File file : GAME_DIR.listFiles()) {
                        if (file.getName().startsWith("Journal.")) {
                            continue; // We have custom Journal logic.
                        }

                        String filename = file.getName().split("\\.")[0];
                        fileListeners.put(filename, new LinkedList<>()); // Create the mapping.
                        fileWatchers.add(
                            (new FileWatcher(file) {
                                @SneakyThrows
                                @Override
                                public void onChange() {
                                    String content = Files.readString(file.toPath());
                                    if (content.length() == 0) {
                                        return; // Can happen on startup. We ignore it :)
                                    }

                                    JsonObject contentJson = Rson.DEFAULT.fromJson(content, JsonObject.class);

//                                    LOGGER.debug("Content change: %s %s", filename, content);
                                    synchronized (LOCK) {
                                        for (ContentListener listener : fileListeners.get(filename)) {
                                            try {
                                                listener.onChange(contentJson);
                                            } catch (Throwable t) {
                                                t.printStackTrace(); // Swallow and move on.
                                            }
                                        }
                                    }
                                }
                            }).run() // Start watching the contents.
                        );
                    }

                    fileListeners.put("Journal", new LinkedList<>()); // Create the mapping.
                    findAndReadJournal();

                    LOGGER.info("Files are being watched! Godspeed CMDR o7");
                    isGameRunning = true;

                    for (GameListener listener : gameListeners) {
                        try {
                            listener.onGameOpen();
                        } catch (Throwable t) {
                            t.printStackTrace(); // Swallow and move on.
                        }
                    }
                } else {
                    LOGGER.info("Game has been closed. Cleaning up resources.");
                    isGameRunning = false;

                    for (GameListener listener : gameListeners) {
                        try {
                            listener.onGameOpen();
                        } catch (Throwable t) {
                            t.printStackTrace(); // Swallow and move on.
                        }
                    }

                    currentJournal = null;

                    for (Thread t : fileWatchers) {
                        t.interrupt();
                    }
                    fileWatchers.clear();

                    for (List<ContentListener> listenerList : fileListeners.values()) {
                        for (ContentListener listener : listenerList) {
                            try {
                                listener.onGameClose();
                            } catch (Throwable t) {
                                t.printStackTrace(); // Swallow and move on.
                            }
                        }
                    }
                    fileListeners.clear();
                }
            }
        }
    };

    private static void findAndReadJournal() throws IOException {
        currentJournal = Files.list(GAME_DIR.toPath())
            .filter((p) -> p.toFile().getName().startsWith("Journal"))
            .max(Comparator.comparingLong(f -> f.toFile().lastModified()))
            .get()
            .toFile();

        fileWatchers.add(
            (new FileWatcher(currentJournal) {
                private int lastLength = 0;

                @SneakyThrows
                @Override
                public void onChange() {
                    // For the journal, we want to notify about the NEW LINES rather than the whole
                    // file. So we grab the contents, and skip forward based on the last known
                    // content length and then split by lines.
                    String content = Files.readString(currentJournal.toPath());
                    int currentLength = content.length();

                    if (currentLength == 0) {
                        return; // Can happen on startup. We ignore it :)
                    }

                    content = content.substring(this.lastLength);
                    String[] contentLines = content.split("\r\n");

                    synchronized (LOCK) {
                        boolean foundContinue = false;

                        for (String line : contentLines) {
                            JsonObject lineJson = Rson.DEFAULT.fromJson(line, JsonObject.class);

                            if (lineJson.getString("event").equalsIgnoreCase("Continued")) {
                                // DO NOT SEND!
                                // We need to find the new journal file and start reading it :)
                                foundContinue = true;
                                continue;
                            }

                            LOGGER.debug("Content change: Journal %s", line);
                            for (ContentListener listener : fileListeners.get("Journal")) {
                                try {
                                    listener.onChange(lineJson);
                                } catch (Throwable t) {
                                    t.printStackTrace(); // Swallow and move on.
                                }
                            }
                        }

                        if (foundContinue) {
                            findAndReadJournal(); // Replaces this instance.
                            fileWatchers.remove(Thread.currentThread()); // Might as well clean this up.
                            Thread.currentThread().interrupt(); // Stop reading this old file.
                        }
                    }

                    this.lastLength = currentLength;
                }
            }).run() // Start watching the contents.
        );
    }

    static void init() {
        process.run();
    }

    public static List<String> getActiveFiles() {
        synchronized (LOCK) {
            return new ArrayList<>(fileListeners.keySet()); // Clone
        }
    }

    public static void registerFileListener(String file, ContentListener listener) {
        synchronized (LOCK) {
            fileListeners.get(file).add(listener);
        }
    }

    public static void unregisterFileListener(String file, ContentListener listener) {
        synchronized (LOCK) {
            if (fileListeners.containsKey(file)) {
                fileListeners.get(file).remove(listener);
            }
        }
    }

    public static void registerGameListener(GameListener listener) {
        synchronized (LOCK) {
            gameListeners.add(listener);
        }
    }

    public static void unregisterGameListener(GameListener listener) {
        synchronized (LOCK) {
            gameListeners.remove(listener);
        }
    }

    public static interface ContentListener {

        public void onChange(JsonObject newContent);

        public void onGameClose();

    }

    public static interface GameListener {

        public void onGameOpen();

        public void onGameClose();

    }

}
