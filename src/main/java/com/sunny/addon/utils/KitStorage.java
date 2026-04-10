package com.sunny.addon.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;

public class KitStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path KITS_FOLDER = Path.of("sunnyaddon", "kits");
    private static final Path LOADED_KIT_FILE = Path.of("sunnyaddon", "loadkit", "loadedkit.txt");
    private static String activeKit = null;
    private static boolean rekitRequested = false;

    public static void saveCurrentKit(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            try {
                Files.createDirectories(KITS_FOLDER);
                JsonObject root = new JsonObject();

                for (int slot = 0; slot < 36; slot++) {
                    ItemStack stack = mc.player.getInventory().getStack(slot);
                    if (!stack.isEmpty()) {
                        Identifier id = Registries.ITEM.getId(stack.getItem());
                        root.addProperty(String.valueOf(slot), id.toString());
                    }
                }

                Path file = KITS_FOLDER.resolve(name.toLowerCase() + ".json");
                try (Writer writer = Files.newBufferedWriter(file)) {
                    GSON.toJson(root, writer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadPersistedKit() {
        if (Files.exists(LOADED_KIT_FILE)) {
            try {
                String name = Files.readString(LOADED_KIT_FILE).trim();
                if (!name.isEmpty()) {
                    Path kitFile = KITS_FOLDER.resolve(name + ".json");
                    if (Files.exists(kitFile)) {
                        activeKit = name;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public static boolean loadKit(String name) {
        Path file = KITS_FOLDER.resolve(name.toLowerCase() + ".json");
        if (!Files.exists(file)) {
            return false;
        } else {
            activeKit = name.toLowerCase();
            try {
                Files.createDirectories(LOADED_KIT_FILE.getParent());
                Files.writeString(LOADED_KIT_FILE, activeKit);
            } catch (Exception ignored) {}
            return true;
        }
    }

    public static boolean hasActiveKit() {
        return activeKit != null && Files.exists(KITS_FOLDER.resolve(activeKit + ".json"));
    }

    public static String getActiveKitName() {
        return activeKit;
    }

    public static void requestRekit() {
        rekitRequested = true;
    }

    public static boolean consumeRekitRequest() {
        boolean value = rekitRequested;
        rekitRequested = false;
        return value;
    }

    public static Map<Integer, String> getActiveKit() {
        if (activeKit == null) return null;

        Path file = KITS_FOLDER.resolve(activeKit + ".json");
        if (!Files.exists(file)) return null;

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<Integer, String> map = new HashMap<>();

            for (Entry<String, JsonElement> entry : root.entrySet()) {
                map.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
            }
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer findTargetSlotForItem(Item item) {
        Map<Integer, String> kit = getActiveKit();
        if (kit == null) return null;

        String itemId = Registries.ITEM.getId(item).toString();

        for (Entry<Integer, String> entry : kit.entrySet()) {
            if (entry.getValue().equals(itemId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean slotAlreadyCorrect(int playerSlot, Item item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        ItemStack stack = mc.player.getInventory().getStack(playerSlot);
        return !stack.isEmpty() && stack.getItem() == item;
    }
}
