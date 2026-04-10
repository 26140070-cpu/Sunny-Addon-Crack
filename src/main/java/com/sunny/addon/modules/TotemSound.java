package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Random;
import javax.sound.sampled.*;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class TotemSound extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> muteOriginal = sgGeneral.add(new BoolSetting.Builder()
        .name("Mute Original")
        .description("Desactiva el sonido original del tótem.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> soundEffect = sgGeneral.add(new BoolSetting.Builder()
        .name("Sound")
        .description("Reproduce un sonido custom al hacer pop.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> randomSound = sgGeneral.add(new BoolSetting.Builder()
        .name("Random Sound")
        .description("Elige un archivo .wav al azar de la carpeta.")
        .defaultValue(false)
        .visible(soundEffect::get)
        .build()
    );

    private final Setting<String> customSoundFile = sgGeneral.add(new StringSetting.Builder()
        .name("Sound File")
        .description("Nombre del archivo .wav (si Random Sound está apagado).")
        .defaultValue("totem.wav")
        .visible(() -> soundEffect.get() && !randomSound.get())
        .build()
    );

    private final Setting<Double> volume = sgGeneral.add(new DoubleSetting.Builder()
        .name("Volume")
        .defaultValue(0.8)
        .min(0.0)
        .max(1.0)
        .visible(soundEffect::get)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("Range")
        .description("Rango máximo para escuchar el sonido.")
        .defaultValue(30)
        .min(5)
        .sliderMax(100)
        .visible(soundEffect::get)
        .build()
    );

    private final File customSoundsDir = new File("sunnyaddon/sounds");
    private final Random random = new Random();

    private static final Field ENTITY_X = findField(Entity.class, "field_6038", "x");
    private static final Field ENTITY_Y = findField(Entity.class, "field_5971", "y");
    private static final Field ENTITY_Z = findField(Entity.class, "field_5989", "z");

    public TotemSound() {
        super(SunnyAddon.CATEGORY, "CustomPopSound", "Sonidos personalizados para los Totem Pops.");
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

    @Override
    public void onActivate() {
        if (!customSoundsDir.exists()) customSoundsDir.mkdirs();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;

        if (packet.getStatus() == 35 && mc.world != null && mc.player != null) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == null) return;

            if (soundEffect.get()) {
                double eX = getRefPos(entity, ENTITY_X);
                double eY = getRefPos(entity, ENTITY_Y);
                double eZ = getRefPos(entity, ENTITY_Z);

                double dx = mc.player.getX() - eX;
                double dy = mc.player.getY() - eY;
                double dz = mc.player.getZ() - eZ;
                double distSq = dx*dx + dy*dy + dz*dz;

                if (distSq <= (range.get() * range.get())) {
                    if (randomSound.get()) playRandomSound();
                    else playCustomSound(customSoundFile.get());
                }
            }
        }
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (muteOriginal.get()) {
            String soundId = event.sound.getId().toString();
            if (soundId.contains("item.totem.use") || soundId.contains("totem")) {
                event.cancel();
            }
        }
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
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float vol = volume.get().floatValue();
                    float dB = (float) (Math.log(vol <= 0 ? 0.0001 : vol) / Math.log(10.0) * 20.0);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }
                clip.start();
                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) clip.close();
                });
                Thread.sleep(clip.getMicrosecondLength() / 1000 + 100);
            } catch (Exception ignored) {}
        }).start();
    }
}
