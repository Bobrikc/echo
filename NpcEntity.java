package com.echocity.velmar;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Житель Вельмара: живёт по расписанию (утро/день/вечер/ночь), говорит по сюжету, слушается эха. */
public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_ID = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final String[] CITIZEN_NAMES = {"Ярин", "Дара", "Борик", "Сева", "Томас", "Элин", "Ворт", "Кира", "Лавр", "Мирон", "Оля", "Гелий", "Нина", "Рем", "Ульна", "Бран", "Ясна", "Фрол", "Тана", "Игнат", "Веста", "Олег", "Мара", "Кассий"};

    private BlockPos origin = BlockPos.ZERO;
    private boolean hasOrigin = false;
    private int talkingTicks = 0;

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.28D).add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID, "citizen");
        builder.define(DATA_SKIN, 1);
    }

    /** Настраивает жителя при появлении. seed — для вариантов горожан. */
    public void setup(NpcDef def, BlockPos origin, int seed) {
        this.entityData.set(DATA_ID, def.id);
        this.entityData.set(DATA_SKIN, 1 + Math.floorMod(seed, 16));
        this.origin = origin;
        this.hasOrigin = true;
        String name = def == NpcDef.CITIZEN ? CITIZEN_NAMES[Math.floorMod(seed, CITIZEN_NAMES.length)] : def.name;
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(def != NpcDef.CITIZEN);   // у прохожих имя видно, только если навести взгляд
        equip(def, seed);
    }

    /** Предмет в руке: стража с мечом, кузнец с топором, пекарша с хлебом и так далее. */
    private void equip(NpcDef d, int seed) {
        Item item = switch (d) {
            case GARTH, GATE_N, GATE_S, GATE_E, GATE_W, PATROL_A, PATROL_B -> Items.IRON_SWORD;
            case DORN -> Items.IRON_AXE;
            case LEYA -> Items.BREAD;
            case OSIP -> Items.FISHING_ROD;
            case FOMA -> Items.WRITABLE_BOOK;
            case ORSEN -> Items.GOLD_INGOT;
            case MIRA, RZHAVY -> Items.STONE_SWORD;
            case BROK -> Items.STICK;
            case HVAT -> Items.STONE_AXE;
            case TAVI -> Items.BOWL;
            case NUNA -> Items.APPLE;
            case CITIZEN -> {
                Item[] pool = {Items.BREAD, Items.APPLE, Items.CARROT, Items.STICK, Items.AIR, Items.AIR, Items.AIR};
                yield pool[Math.floorMod(seed, pool.length)];
            }
            default -> Items.AIR;
        };
        this.setItemSlot(EquipmentSlot.MAINHAND, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
    }

    public NpcDef def() { return NpcDef.byId(this.entityData.get(DATA_ID)); }

    public String npcName() { return getName().getString(); }

    /** Имя файла скина без расширения: нужно и для отрисовки, и для портрета в окне диалога. */
    public String skinName() {
        NpcDef d = def();
        return d == NpcDef.CITIZEN ? "citizen" + this.entityData.get(DATA_SKIN) : d.skin;
    }

    public ResourceLocation skin() {
        return ResourceLocation.fromNamespaceAndPath(EchoCity.MODID, "textures/entity/" + skinName() + ".png");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ScheduleGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            this.talkingTicks = 240;
            this.swing(InteractionHand.MAIN_HAND);
            this.getNavigation().stop();
            this.getLookControl().setLookAt(sp, 30.0F, 30.0F);
            if (sp.isShiftKeyDown() && sp.getMainHandItem().is(ModItems.ECHO_SHELL.get())) StoryEngine.listen(sp, this);
            else StoryEngine.talk(sp, this);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (talkingTicks > 0) talkingTicks--;
        if (!this.level().isClientSide && this.tickCount % 240 == 0 && this.random.nextInt(3) == 0) {
            Player near = this.level().getNearestPlayer(this, 5.0D);
            if (near instanceof ServerPlayer sp) {
                String line = StoryData.ambient(def());
                Payload.send(sp, Payload.bubble(npcName(), skinName(), line, false),
                    Component.literal("[" + npcName() + "] " + line).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", this.entityData.get(DATA_ID));
        tag.putInt("NpcSkin", this.entityData.get(DATA_SKIN));
        tag.putBoolean("HasOrigin", hasOrigin);
        tag.putInt("Ox", origin.getX()); tag.putInt("Oy", origin.getY()); tag.putInt("Oz", origin.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcId")) this.entityData.set(DATA_ID, tag.getString("NpcId"));
        if (tag.contains("NpcSkin")) this.entityData.set(DATA_SKIN, tag.getInt("NpcSkin"));
        this.hasOrigin = tag.getBoolean("HasOrigin");
        this.origin = new BlockPos(tag.getInt("Ox"), tag.getInt("Oy"), tag.getInt("Oz"));
    }

    /** Ходит между точками расписания: у каждого жителя своя жизнь по времени суток. */
    static class ScheduleGoal extends Goal {
        private final NpcEntity npc;
        private int cooldown = 0;
        private Vec3 target;

        ScheduleGoal(NpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!npc.hasOrigin || npc.talkingTicks > 0) return false;
            if (--cooldown > 0) return false;
            cooldown = 60 + npc.random.nextInt(60);
            long t = npc.level().getDayTime();
            NpcDef d = npc.def();
            BlockPos rel = npc.blockPosition().subtract(npc.origin);
            int[] w = d.wanders() ? NpcDef.citizenSpot(t, new java.util.Random(npc.random.nextLong()), rel.getX(), rel.getZ()) : d.waypoint(t);
            BlockPos goal = npc.origin.offset(w[0], 0, w[1]);
            if (npc.blockPosition().distSqr(goal) < 6.0D) return false;
            target = Vec3.atBottomCenterOf(goal);
            return true;
        }

        @Override
        public boolean canContinueToUse() { return !npc.getNavigation().isDone() && npc.talkingTicks <= 0; }

        @Override
        public void start() { npc.getNavigation().moveTo(target.x, target.y, target.z, 0.9D); }

        @Override
        public void stop() { npc.getNavigation().stop(); }
    }
}
