package xyz.e3ndr.ed_localapi.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;

//Modified from: https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
@AllArgsConstructor
public abstract class FileWatcher {
    private File file;

    public abstract void onChange();

    public Thread run() {
        Thread t = new Thread(this::runLoop);
        t.setDaemon(true);
        t.setName("ProcessWatcher: " + this.file);
        t.start();
        return t;
    }

    @SuppressWarnings("unchecked")
    private void runLoop() {
        this.onChange(); // Initial trigger.

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = this.file.getCanonicalFile().getParentFile().toPath();
            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

            while (!Thread.interrupted()) {
                WatchKey key = watcher.poll(25, TimeUnit.MILLISECONDS);

                if (key == null) {
                    Thread.yield();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    WatchEvent.Kind<?> kind = event.kind();
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                        && filename.toString().equals(this.file.getName())) {
                            this.onChange();
                        }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }
        } catch (InterruptedException ignored) {} catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
