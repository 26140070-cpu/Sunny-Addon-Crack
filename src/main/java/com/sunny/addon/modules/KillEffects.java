package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import javax.sound.sampled.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class KillEffects extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");

    private final Setting<Boolean> lightningEffect = sgGeneral.add(new BoolSetting.Builder()
        .name("Lightning")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> soundEffect = sgSound.add(new BoolSetting.Builder()
        .name("Sound")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> randomsound = sgSound.add(new BoolSetting.Builder()
        .name("Random Sound")
        .defaultValue(false)
        .visible(soundEffect::get)
        .build()
    );

    private final Setting<String> customSoundFile = sgSound.add(new StringSetting.Builder()
        .name("Sound File")
        .defaultValue("GetOut.wav")
        .visible(() -> soundEffect.get() && !randomsound.get())
        .build()
    );

    private final Setting<Double> volume = sgSound.add(new DoubleSetting.Builder()
        .name("Volume")
        .defaultValue(0.8)
        .min(0.0)
        .max(1.0)
        .visible(soundEffect::get)
        .build()
    );

    private final Setting<Integer> rangeenemi = sgSound.add(new IntSetting.Builder()
        .name("Range")
        .defaultValue(30)
        .min(5)
        .max(100)
        .visible(soundEffect::get)
        .build()
    );

    private final Map<UUID, Float> lastHealthMap = new LinkedHashMap<>();
    private final File customSoundsDir = new File("sunnyaddon/sounds");
    private final Random random = new Random();

    private static final Field ENTITY_X = findField(Entity.class, "field_6038", "x");
    private static final Field ENTITY_Y = findField(Entity.class, "field_5971", "y");
    private static final Field ENTITY_Z = findField(Entity.class, "field_5989", "z");
    private static final Field LIVING_HEALTH = findField(LivingEntity.class, "field_6213", "health");
    private static final Field LIGHTNING_COSMETIC = findField(LightningEntity.class, "field_7182", "cosmetic");

    public KillEffects() {
        super(SunnyAddon.CATEGORY, "Kill Effects", "Efectos al matar jugadores.");
    }

    private static Field findField(Class<?> clazz, String intermediary, String yarn) {
        try {
            Field f = clazz.getDeclaredField(intermediary);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            try {
                Field f = clazz.getDeclaredField(yarn);
                f.setAccessible(true);
                return f;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private double getRefPos(Entity e, Field f) {
        try { return f.getDouble(e); } catch (Exception ex) { return 0; }
    }

    private float getRefHealth(LivingEntity e) {
        if (LIVING_HEALTH == null) return e.getHealth();
        try { return LIVING_HEALTH.getFloat(e); } catch (Exception ex) { return e.getHealth(); }
    }

    @Override
    public void onActivate() {
        lastHealthMap.clear();
        if (!customSoundsDir.exists()) customSoundsDir.mkdirs();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player) {
                PlayerEntity target = (PlayerEntity) entity;
                UUID id = target.getUuid();
                float currentHealth = getRefHealth(target);
                Float previousHealth = lastHealthMap.get(id);

                if (previousHealth != null && previousHealth > 0.0f && currentHealth <= 0.0f) {
                    onKill(target);
                }

                if (currentHealth <= 0.0f) lastHealthMap.remove(id);
                else lastHealthMap.put(id, currentHealth);
            }
        }
    }

    private void onKill(PlayerEntity victim) {
        double dX = getRefPos(victim, ENTITY_X);
        double dY = getRefPos(victim, ENTITY_Y);
        double dZ = getRefPos(victim, ENTITY_Z);

        if (mc.player.distanceTo(victim) <= rangeenemi.get()) {
            if (lightningEffect.get()) {
                spawnLightning(dX, dY, dZ);
            }
            if (soundEffect.get()) {
                if (randomsound.get()) playRandomSound();
                else playCustomSound(customSoundFile.get());
            }
        }
    }

    private void spawnLightning(double x, double y, double z) {
        if (mc.world == null) return;
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        try {
            if (LIGHTNING_COSMETIC != null) LIGHTNING_COSMETIC.setBoolean(lightning, true);
        } catch (Exception ignored) {}

        lightning.updatePosition(x, y, z);

        mc.world.addEntity(lightning);
    }

    private void playRandomSound() {
        File[] files = customSoundsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        if (files == null || files.length == 0) return;
        File randomFile = files[random.nextInt(files.length)];
        playCustomSound(randomFile.getName());
    }

    private void playCustomSound(String fileName) {
        File file = new File(customSoundsDir, fileName);
        if (!file.exists()) return;

        new Thread(() -> {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float vol = volume.get().floatValue();
                    float dB = (float) (Math.log(vol <= 0 ? 0.0001 : vol) / Math.log(10.0) * 20.0);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) clip.close();
                });
            } catch (Exception ignored) {}
        }).start();
    }
}
