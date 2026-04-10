package com.sunny.addon;

import com.mojang.logging.LogUtils;
import com.sunny.addon.commands.autopilotcommand;
import com.sunny.addon.commands.kit;
import com.sunny.addon.hud.Watermark;
import com.sunny.addon.modules.*;
import com.sunny.addon.utils.KitStorage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class SunnyAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Sunny");
    public static final HudGroup HUD_GROUP = new HudGroup("Sunny HUD");

    @Override
    public void onInitialize() {
        MeteorClient.EVENT_BUS.subscribe(this);
        LOG.info("Initializing Meteor Sunny Addon");
        KitStorage.loadPersistedKit();
        this.registerModulesFromPackage("com.sunny.addon.modules");
        Commands.add(new autopilotcommand());
        Commands.add(new kit());
        Modules.get().add(new AnchorAura());
        Modules.get().add(new AntiSelfPoP());
        Modules.get().add(new AutoFeetTrap());
        Modules.get().add(new autopilot());
        Modules.get().add(new BetterAutoTotem());
        Modules.get().add(new ChinaHat());
        Modules.get().add(new CrystalPlus());
        Modules.get().add(new TotemSound());
        Modules.get().add(new Dupes());
        Modules.get().add(new JumpCircle());
        Modules.get().add(new KillEffects());
        Modules.get().add(new KomarPvP());
        Modules.get().add(new MaceKill());
        Modules.get().add(new ProjectileTeleport());
        Modules.get().add(new RPC());
        Modules.get().add(new ShulkerStealer());
        Modules.get().add(new SunnyCape());
        Modules.get().add(new HeliumOnTop());
        Hud.get().register(Watermark.MARKTEXT);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    public String getPackage() {
        return "com.sunny.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-sunnyaddon");
    }

    private void registerModulesFromPackage(String pkg) {
        String path = pkg.replace('.', '/');
        try {
            Enumeration<URL> resources = this.getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    File dir = new File(resource.toURI());
                    File[] files = dir.listFiles((d, namex) -> namex.endsWith(".class"));
                    if (files != null) {
                        for (File file : files) {
                            String className = pkg + "." + file.getName().substring(0, file.getName().length() - 6);
                            this.tryLoadAndRegister(className);
                        }
                    }
                } else if ("jar".equals(protocol)) {
                    String fullPath = resource.getPath();
                    String jarPath = fullPath.substring(fullPath.indexOf("file:"), fullPath.indexOf("!"));
                    jarPath = jarPath.startsWith("file:") ? jarPath.substring(5) : jarPath;
                    try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.replace('/', '.').substring(0, name.length() - 6);
                                this.tryLoadAndRegister(className);
                            }
                        }
                    }
                }
            }
        } catch (Exception var15) {
            LOG.error("Error scanning modules package: {}", pkg, var15);
        }
    }

    private void tryLoadAndRegister(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (Module.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Module module = (Module)ctor.newInstance();
                Modules.get().add(module);
                LOG.info("Registered module: {}", className);
            }
        } catch (Throwable var5) {
            LOG.warn("Failed to register module: {}", className, var5);
        }
    }
}
