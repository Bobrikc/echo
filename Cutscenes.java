package com.echocity.velmar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Мини-катсцены: камера (невидимая стойка для брони) плывёт по заданным кадрам, а клиент с модом
 * рисует чёрные полосы и титры. Игрок на время становится невидимым и неуязвимым.
 */
public final class Cutscenes {
    private static final Map<UUID, CameraStand> ACTIVE = new HashMap<>();

    public record Caption(String title, String sub, int ticks) {}

    /** Один кадр: путь камеры pos(t), t от 0 до 1, взгляд плавно переходит от lookA к lookB. */
    public record Shot(DoubleFunction<Vec3> pos, Vec3 lookA, Vec3 lookB, int ticks) {}

    private Cutscenes() {}

    public static boolean active(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    public static void skip(ServerPlayer p) {
        CameraStand c = ACTIVE.get(p.getUUID());
        if (c != null) c.finish();
    }

    // ---------- конструкторы кадров ----------
    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) { return a.add(b.subtract(a).scale(t)); }

    public static Shot dolly(Vec3 a, Vec3 b, Vec3 lookA, Vec3 lookB, int ticks) {
        return new Shot(t -> lerp(a, b, t), lookA, lookB, ticks);
    }

    public static Shot orbit(Vec3 center, double radius, double height, double deg0, double deg1, Vec3 look, int ticks) {
        return new Shot(t -> {
            double a = Math.toRadians(deg0 + (deg1 - deg0) * t);
            return new Vec3(center.x + Math.cos(a) * radius, center.y + height, center.z + Math.sin(a) * radius);
        }, look, look, ticks);
    }

    private static Vec3 rel(BlockPos o, double x, double y, double z) {
        return new Vec3(o.getX() + 0.5 + x, o.getY() + y, o.getZ() + 0.5 + z);
    }

    // ---------- запуск ----------
    /** Проигрывает катсцену: вылет камеры от глаз игрока, кадры сцены, возврат к игроку. */
    public static void play(ServerPlayer p, List<Caption> caps, List<Shot> scene, Runnable onFinish) {
        if (active(p) || !(p.level() instanceof ServerLevel lvl) || scene.isEmpty()) {
            onFinish.run();
            return;
        }
        int captionTotal = 0;
        for (Caption c : caps) captionTotal += c.ticks();
        int sceneSum = 0;
        for (Shot s : scene) sceneSum += s.ticks();
        int budget = Math.max(60, captionTotal - 80);

        Vec3 eye = p.getEyePosition();
        Vec3 fwd = eye.add(p.getViewVector(1.0F).scale(6.0));
        Shot first = scene.get(0), last = scene.get(scene.size() - 1);
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot(t -> lerp(eye, first.pos().apply(0.0), t), fwd, first.lookA(), 40));
        for (Shot s : scene) shots.add(new Shot(s.pos(), s.lookA(), s.lookB(), Math.max(10, s.ticks() * budget / sceneSum)));
        shots.add(new Shot(t -> lerp(last.pos().apply(1.0), eye, t), last.lookB(), fwd, 40));

        int total = 0;
        for (Shot s : shots) total += s.ticks();

        List<String[]> wire = new ArrayList<>();
        for (Caption c : caps) wire.add(new String[]{c.title(), c.sub(), String.valueOf(c.ticks())});

        CameraStand stand = new CameraStand(lvl, p, shots, total, onFinish, eye);
        ACTIVE.put(p.getUUID(), stand);
        lvl.addFreshEntity(stand);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, total + 120, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, total + 120, 4, false, false));
        Payload.send(p, Payload.cutscene(wire), null);
    }

    public static void playIntro(ServerPlayer p) {
        BlockPos o = PlayerState.origin(p);
        List<Caption> caps = List.of(
            new Caption("ВЕЛЬМАР", "Город, где колокол помнит всё", 130),
            new Caption("", "Раз в семь лет колокол звонит эхом. Тот, кто его слышит, слышит чужие мысли.", 170),
            new Caption("", "Ты очнулся в таверне «Тихий колокол». С раковиной в руке и без памяти.", 150),
            new Caption("", "Поговори с Тави. Она ждёт.", 90));
        List<Shot> scene = List.of(
            orbit(rel(o, 0, 0, 0), 55, 52, 200, 285, rel(o, 0, 6, 0), 220),
            dolly(rel(o, 14, 17, -22), rel(o, -3, 3, -9), rel(o, -10, 2, -11), rel(o, -10, 1.6, -11), 130),
            dolly(rel(o, -8, 1.9, -11), rel(o, -12.5, 1.9, -10), rel(o, -15, 1.6, -9), rel(o, -15, 1.6, -9), 100));
        play(p, caps, scene, () -> {
            // после вступления сразу открывается разговор с Тави
            PlayerState.tag(p).putString("talk_skin", "tavi");
            StoryEngine.show(p, "tavi_intro");
        });
    }

    public static void playEnding(ServerPlayer p, StoryNode node) {
        if (!PlayerState.hasCity(p)) {
            StoryEngine.finishEnding(p, node);
            return;
        }
        BlockPos o = PlayerState.origin(p);
        List<Caption> caps = new ArrayList<>();
        for (String line : node.text.split("\n"))
            caps.add(new Caption("", line, Math.max(110, Math.min(260, 60 + line.length() * 2))));
        caps.add(new Caption(node.endTitle, node.endSubtitle, 170));
        play(p, caps, endingScene(o, node.id), () -> StoryEngine.finishEnding(p, node));
    }

    private static List<Shot> endingScene(BlockPos o, String id) {
        switch (id) {
            case "law_good":
                return List.of(
                    orbit(rel(o, 13, 0, 9), 26, 11, 170, 250, rel(o, 13, 4, 9), 260),
                    orbit(rel(o, 0, 0, 0), 38, 24, 40, 120, rel(o, 0, 5, 0), 200));
            case "law_bad":
                return List.of(
                    orbit(rel(o, 13, 0, 14), 22, 8, 300, 200, rel(o, 13, 5, 14), 260),
                    dolly(rel(o, 30, 14, 20), rel(o, 18, 6, 4), rel(o, 13, 4, 12), rel(o, 13, 5, 14), 200));
            case "mira_raid":
                return List.of(
                    orbit(rel(o, 0, 0, -19), 22, 10, 90, 180, rel(o, 0, 10, -19), 260),
                    orbit(rel(o, 0, 0, -19), 30, 20, 180, 290, rel(o, 0, 8, -19), 200));
            case "silence_end":
                return List.of(
                    orbit(rel(o, 0, 0, -19), 14, 12, 0, 160, rel(o, 0, 10, -19), 260),
                    dolly(rel(o, 0, 24, -34), rel(o, 0, 16, -26), rel(o, 0, 10, -19), rel(o, 0, 10, -19), 200));
            case "orsen_book":
                return List.of(
                    orbit(rel(o, 13, 0, 14), 18, 7, 20, 110, rel(o, 13, 4, 14), 260),
                    dolly(rel(o, -5, 10, 4), rel(o, 8, 4, 6), rel(o, 13, 3, 10), rel(o, 13, 3, 10), 200));
            default:
                return List.of(orbit(rel(o, 0, 0, 0), 45, 30, 0, 90, rel(o, 0, 5, 0), 300));
        }
    }

    // ---------- камера ----------
    private static final class CameraStand extends ArmorStand {
        private final ServerPlayer player;
        private final ServerLevel lvl;
        private final List<Shot> shots;
        private final int total;
        private final Runnable onFinish;
        private final Vec3 savedPos;
        private final float savedYaw, savedPitch;
        private int step = 0;
        private boolean cameraSet = false, done = false;

        CameraStand(ServerLevel lvl, ServerPlayer p, List<Shot> shots, int total, Runnable onFinish, Vec3 eye) {
            super(lvl, eye.x, eye.y, eye.z);
            this.lvl = lvl;
            this.player = p;
            this.shots = shots;
            this.total = total;
            this.onFinish = onFinish;
            this.savedPos = p.position();
            this.savedYaw = p.getYRot();
            this.savedPitch = p.getXRot();
            this.setInvisible(true);
            this.setNoGravity(true);
            this.setInvulnerable(true);
            this.setPos(eye.x, eye.y - this.getEyeHeight(), eye.z);
        }

        @Override
        public void tick() {
            if (done) return;
            if (player.isRemoved() || !player.isAlive() || player.level() != lvl) {
                finish();
                return;
            }
            if (!cameraSet && step >= 2) {   // небольшая задержка: клиент должен успеть получить сущность
                player.setCamera(this);
                cameraSet = true;
            }
            if (step >= total) {
                finish();
                return;
            }
            int acc = 0;
            for (Shot s : shots) {
                if (step < acc + s.ticks()) {
                    apply(s, (step - acc + 1.0) / s.ticks());
                    break;
                }
                acc += s.ticks();
            }
            step++;
        }

        private void apply(Shot s, double t) {
            double e = smooth(Math.min(1.0, t));
            Vec3 pos = s.pos().apply(e);
            Vec3 look = lerp(s.lookA(), s.lookB(), e);
            this.setPos(pos.x, pos.y - this.getEyeHeight(), pos.z);
            double dx = look.x - pos.x, dy = look.y - pos.y, dz = look.z - pos.z;
            float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.setYHeadRot(yaw);
        }

        void finish() {
            if (done) return;
            done = true;
            ACTIVE.remove(player.getUUID());
            player.setCamera(player);
            player.teleportTo(lvl, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
            player.removeEffect(MobEffects.INVISIBILITY);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            Payload.send(player, Payload.close(), null);
            this.discard();
            onFinish.run();
        }
    }
}
