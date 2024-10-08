package xyz.e3ndr.ed_localapi.util;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import co.casterlabs.commons.io.streams.StreamUtil;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class ProcessWatcher {
    public final String target; // MyApp.exe

    public Thread run() {
        Thread t = new Thread(this::runLoop);
        t.setDaemon(true);
        t.setName("ProcessWatcher: " + this.target);
        t.start();
        return t;
    }

    private void runLoop() {
        try {
            boolean lastState = false;
            while (!Thread.interrupted()) {
                String result = StreamUtil.toString(
                    Runtime.getRuntime().exec(
                        String.format(
                            "tasklist /fi \"ImageName eq %s\" /fo csv", this.target
                        )
                    ).getInputStream(),
                    Charset.defaultCharset()
                );

                String lines[] = result.split("\r\n");

                // First line is always the column labels. So if a process exists with that
                // executable name then the lines length will be greater than 1.
                boolean currentState = lines.length > 1;
                if (currentState != lastState) {
                    // Broadcast a change.
                    this.onChange(currentState);
                    lastState = currentState;
                }

                TimeUnit.SECONDS.sleep(5);
            }
        } catch (InterruptedException ignored) {} catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public abstract void onChange(boolean currentState);

}
