package xyz.e3ndr.ed_localapi;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.sora.Sora;
import co.casterlabs.sora.SoraFramework;
import co.casterlabs.sora.SoraLauncher;
import co.casterlabs.sora.api.SoraPlugin;
import co.casterlabs.sora.plugins.SoraPlugins;
import lombok.NonNull;
import xyz.e3ndr.ed_localapi.routes.RouteFile;
import xyz.e3ndr.ed_localapi.routes.RouteGame;
import xyz.e3ndr.ed_localapi.routes.RouteMeta;

public class Main {
    public static final int PORT = Integer.parseInt(System.getProperty("edla.port", "10986"));
    public static final String HOSTNAME = System.getProperty("edla.hostname", "localhost"); // Note: changing to anything besides localhost requires you either disable
                                                                                            // websecurity or setup a TLS proxy. I will not help you with this.

    public static void main(String[] args) throws IOException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);

        TrayHandler.tryCreateTray();

        SoraFramework sFramework = new SoraLauncher()
            .setBindAddress(HOSTNAME)
            .setPort(PORT)
            .setDebug(false)
            .buildWithoutPluginLoader();

        SoraPlugins sora = sFramework.getSora();
        DummyPlugin plugin = new DummyPlugin();

        sora.register(plugin);
        sora.addProvider(plugin, new RouteMeta());
        sora.addProvider(plugin, new RouteFile());
        sora.addProvider(plugin, new RouteGame());

        sFramework.startHttpServer();

        EliteDangerous.init(); // Init.
    }

    private static class DummyPlugin extends SoraPlugin {

        @Override
        public void onInit(Sora sora) {}

        @Override
        public void onClose() {}

        @Override
        public @Nullable String getVersion() {
            return null;
        }

        @Override
        public @Nullable String getAuthor() {
            return null;
        }

        @Override
        public @NonNull String getName() {
            return "ED-LocalAPI";
        }

        @Override
        public @NonNull String getId() {
            return "xyz.e3ndr.ed_localapi";
        }

    }

}
