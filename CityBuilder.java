package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Строит город Вельмар вокруг точки origin (центр площади; y — уровень ног игрока).
 * Координаты: +x восток, +z юг. Размер 201x201 блоков (стена по периметру), ядро города в центре.
 */
public final class CityBuilder {
    private final ServerLevel lv;
    private final BlockPos o;

    public CityBuilder(ServerLevel lv, BlockPos origin) {
        this.lv = lv;
        this.o = origin;
    }

    // ---------- примитивы ----------
    private void put(int x, int y, int z, Block b) { lv.setBlock(o.offset(x, y, z), b.defaultBlockState(), 2); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    put(x, y, z, b);
    }

    /** Полая постройка: пол на y=-1, стены 0..h-1, крыша на y=h, угловые столбы. */
    private void building(int x1, int z1, int x2, int z2, int h, Block wall, Block floor, Block roof, Block corner) {
        fill(x1, -1, z1, x2, -1, z2, floor);
        fill(x1, 0, z1, x2, h - 1, z2, wall);
        fill(x1 + 1, 0, z1 + 1, x2 - 1, h - 1, z2 - 1, Blocks.AIR);
        fill(x1, h, z1, x2, h, z2, roof);
        if (corner != null) {
            fill(x1, 0, z1, x1, h - 1, z1, corner);
            fill(x2, 0, z1, x2, h - 1, z1, corner);
            fill(x1, 0, z2, x1, h - 1, z2, corner);
            fill(x2, 0, z2, x2, h - 1, z2, corner);
        }
    }

    private void doorway(int x, int z) { fill(x, 0, z, x, 1, z, Blocks.AIR); }

    private void window(int x, int y, int z) { put(x, y, z, Blocks.GLASS); }

    private void lamp(int x, int z) {
        put(x, 0, z, Blocks.OAK_FENCE);
        put(x, 1, z, Blocks.OAK_FENCE);
        put(x, 2, z, Blocks.LANTERN);
    }

    /** Жилой дом 5x5. side: 'N','S','E','W' — сторона двери. */
    private void house(int x1, int z1, int x2, int z2, char side) {
        building(x1, z1, x2, z2, 4, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.OAK_LOG);
        int cx = (x1 + x2) / 2, cz = (z1 + z2) / 2;
        switch (side) {
            case 'N' -> doorway(cx, z1);
            case 'S' -> doorway(cx, z2);
            case 'W' -> doorway(x1, cz);
            default -> doorway(x2, cz);
        }
        window(x1, 2, cz); window(x2, 2, cz);
        put(cx, 0, cz, Blocks.CRAFTING_TABLE);
        put(x1 + 1, 0, z1 + 1, Blocks.OAK_FENCE); put(x1 + 1, 1, z1 + 1, Blocks.LANTERN);
    }

    // ---------- сам город ----------
    public void build() {
        // 1. подготовка местности: срезаем холмы, засыпаем ямы до твёрдой земли
        prepareTerrain();

        // 2. улицы и площадь
        fill(-30, -1, -2, 38, -1, 2, Blocks.COBBLESTONE);      // главная улица
        fill(-2, -1, -26, 2, -1, 26, Blocks.COBBLESTONE);      // улица к колокольне
        fill(-5, -1, -5, 5, -1, 5, Blocks.STONE_BRICKS);       // площадь
        fill(-1, 0, -1, 1, 0, 1, Blocks.COBBLESTONE);          // колодец
        put(0, -1, 0, Blocks.WATER);
        put(0, 0, 0, Blocks.AIR);
        fill(6, -1, -14, 22, -1, -4, Blocks.COARSE_DIRT);      // рынок
        fill(24, -1, -4, 28, -1, 22, Blocks.GRAVEL);           // переулок

        // 3. таверна «Тихий колокол»: x -20..-10, z -16..-6
        building(-20, -16, -10, -6, 4, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.OAK_LOG);
        doorway(-10, -11);
        window(-16, 1, -16); window(-14, 1, -16); window(-20, 1, -11); window(-10, 2, -8);
        fill(-19, 0, -14, -15, 0, -14, Blocks.BARREL);          // стойка
        put(-18, 0, -11, Blocks.OAK_SLAB); put(-17, 0, -11, Blocks.OAK_SLAB); // столы
        fill(-19, 0, -8, -19, 0, -7, Blocks.RED_WOOL);          // койки
        lamp(-19, -15); lamp(-11, -15); lamp(-11, -7); lamp(-19, -10);
        gable(-20, -16, -10, -6, 4, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, false);
        fill(-14, 0, -15, -12, 1, -15, Blocks.BOOKSHELF);        // книжные полки
        fill(-17, 0, -13, -13, 0, -13, Blocks.RED_CARPET);       // ковровая дорожка
        put(-19, 0, -12, Blocks.CAMPFIRE);                       // очаг

        // 4. рынок: прилавки под навесами
        for (int x : new int[]{8, 12, 16, 20}) {
            fill(x, 0, -13, x + 2, 1, -11, Blocks.AIR);
            put(x, 0, -13, Blocks.OAK_FENCE); put(x + 2, 0, -13, Blocks.OAK_FENCE);
            put(x, 1, -13, Blocks.OAK_FENCE); put(x + 2, 1, -13, Blocks.OAK_FENCE);
            fill(x, 2, -13, x + 2, 2, -11, (x % 8 == 0) ? Blocks.RED_WOOL : Blocks.YELLOW_WOOL);
            fill(x, 0, -11, x + 2, 0, -11, Blocks.BARREL);
            Block[] goods = {Blocks.MELON, Blocks.PUMPKIN, Blocks.HAY_BLOCK, Blocks.COMPOSTER};
            put(x, 1, -11, goods[((x - 8) / 4) % 4]); put(x + 2, 1, -11, goods[((x - 8) / 4 + 1) % 4]);
        }
        lamp(6, -5); lamp(22, -5);

        // 5. гильдия зерна: x 6..20, z 8..20
        building(6, 8, 20, 20, 5, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_ANDESITE);
        doorway(13, 8); fill(13, 2, 8, 13, 2, 8, Blocks.AIR);
        fill(11, 3, 8, 15, 3, 8, Blocks.RED_WOOL);
        for (int x : new int[]{9, 17}) { window(x, 2, 8); window(x, 2, 20); }
        fill(8, 0, 18, 18, 0, 18, Blocks.BARREL);               // «пустые» бочки
        put(13, 0, 12, Blocks.CHEST);
        fill(13, 0, 9, 13, 0, 11, Blocks.RED_CARPET);
        fill(7, 0, 10, 7, 2, 16, Blocks.BOOKSHELF);
        fill(10, 0, 12, 11, 0, 12, Blocks.OAK_SLAB);
        lamp(8, 10); lamp(18, 10);

        // 6. казармы стражи: x -20..-10, z 8..18
        building(-20, 8, -10, 18, 4, Blocks.COBBLESTONE, Blocks.STONE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS);
        doorway(-10, 13);
        window(-20, 2, 13); window(-15, 2, 8); window(-15, 2, 18);
        fill(-19, 0, 10, -18, 0, 10, Blocks.RED_WOOL);
        put(-17, 0, 16, Blocks.CRAFTING_TABLE);
        lamp(-11, 11); lamp(-11, 15);

        // 7. колокольня: x -3..3, z -22..-16, высота 14
        building(-3, -22, 3, -16, 14, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.STONE_BRICKS);
        doorway(0, -16);
        fill(-1, 9, -22, 1, 12, -22, Blocks.AIR);               // открытая звонница
        fill(-1, 9, -16, 1, 12, -16, Blocks.AIR);
        fill(-3, 9, -20, -3, 12, -18, Blocks.AIR);
        fill(3, 9, -20, 3, 12, -18, Blocks.AIR);
        put(0, 10, -19, Blocks.GOLD_BLOCK);                     // колокол
        put(0, 11, -19, Blocks.OAK_FENCE);
        fill(-2, 0, -21, -2, 8, -21, Blocks.LADDER);
        lamp(-4, -15); lamp(4, -15);

        // 8. переулок: стены и хижина Миры
        fill(23, 0, -4, 23, 4, 22, Blocks.STONE_BRICKS);
        fill(29, 0, -4, 29, 4, 13, Blocks.STONE_BRICKS);
        building(29, 14, 37, 22, 4, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_LOG);
        doorway(29, 18);
        fill(31, 0, 20, 32, 0, 20, Blocks.HAY_BLOCK);
        put(34, 0, 16, Blocks.BARREL); put(35, 0, 16, Blocks.BARREL);
        put(26, 0, 6, Blocks.BARREL); put(25, 0, 9, Blocks.BARREL);
        lamp(24, 3);
        gable(29, 14, 37, 22, 4, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, false);

        // 9. жилые дома по краям
        house(-8, -14, -4, -10, 'S');
        house(-9, -22, -5, -18, 'S');
        house(5, -22, 9, -18, 'S');
        house(-8, 6, -4, 10, 'N');
        house(6, 22, 10, 26, 'N');
        house(14, 22, 18, 26, 'N');
        house(-27, -3, -23, 1, 'E');
        house(-27, 3, -23, 7, 'N');
        house(-27, -12, -23, -8, 'S');
        house(31, -12, 35, -8, 'S');
        house(31, 6, 35, 10, 'N');

        // 10. уличные фонари
        int[][] ls = {{3,3},{-3,3},{3,-3},{-3,-3},{11,3},{-11,3},{19,3},{-19,3},{3,11},{3,-11},{-3,15},{3,19},{31,3},{-3,-24}};
        for (int[] l : ls) lamp(l[0], l[1]);

        // 11. внешний город: стена с воротами, река с мостами, кварталы, деревья
        outerCity();
    }


    // ---------- местность ----------
    private void prepareTerrain() {
        int top = Math.min(26, lv.getMaxBuildHeight() - 2 - o.getY());
        fill(-HALF, 0, -HALF, HALF, top, HALF, Blocks.AIR);
        fill(-HALF, -1, -HALF, HALF, -1, HALF, Blocks.GRASS_BLOCK);
        int lowest = Math.max(-80, lv.getMinBuildHeight() + 1 - o.getY());
        for (int x = -HALF; x <= HALF; x++)
            for (int z = -HALF; z <= HALF; z++)
                for (int y = -2; y >= lowest; y--) {
                    BlockPos pos = o.offset(x, y, z);
                    BlockState st = lv.getBlockState(pos);
                    if (!st.isAir() && st.getFluidState().isEmpty()) break;
                    lv.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                }
    }

    // ---------- внешний город ----------
    private final List<int[]> blocked = new ArrayList<>();

    private void reserve(int x1, int z1, int x2, int z2) { blocked.add(new int[]{x1, z1, x2, z2}); }

    private boolean isFree(int x1, int z1, int x2, int z2) {
        for (int[] b : blocked) if (x1 <= b[2] && x2 >= b[0] && z1 <= b[3] && z2 >= b[1]) return false;
        return true;
    }

    private void leaf(int x, int y, int z) {
        BlockPos pos = o.offset(x, y, z);
        if (lv.getBlockState(pos).isAir())
            lv.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
    }

    private void tree(int x, int z, int h) {
        fill(x, 0, z, x, h - 1, z, Blocks.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int dy = h - 2; dy <= h + 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (dy == h + 1 && (Math.abs(dx) > 1 || Math.abs(dz) > 1)) continue;
                    leaf(x + dx, dy, z + dz);
                }
    }

    private void wallTower(int cx, int cz, int r, int h) {
        building(cx - r, cz - r, cx + r, cz + r, h, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_ANDESITE);
        for (int i = -r; i <= r; i += 2) {
            put(cx + i, h + 1, cz - r, Blocks.STONE_BRICKS); put(cx + i, h + 1, cz + r, Blocks.STONE_BRICKS);
            put(cx - r, h + 1, cz + i, Blocks.STONE_BRICKS); put(cx + r, h + 1, cz + i, Blocks.STONE_BRICKS);
        }
        window(cx, 3, cz - r); window(cx, 3, cz + r); window(cx - r, 3, cz); window(cx + r, 3, cz);
        put(cx, 0, cz, Blocks.LANTERN);
    }

    private static final Block[][] STYLES = {
        {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.OAK_LOG, Blocks.DARK_OAK_STAIRS},
        {Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.OAK_LOG, Blocks.SPRUCE_STAIRS},
        {Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_BRICK_STAIRS},
        {Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_STAIRS},
        {Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.BRICKS, Blocks.OAK_LOG, Blocks.BRICK_STAIRS}};

    private void stairs(int x, int y, int z, Block b, Direction facing) {
        lv.setBlock(o.offset(x, y, z), b.defaultBlockState().setValue(StairBlock.FACING, facing), 2);
    }

    /**
     * Двускатная крыша со ступенями, начиная с высоты y0 над прямоугольником (x1,z1)-(x2,z2).
     * ridgeAlongZ = true: конёк идёт вдоль оси Z, скаты смотрят на запад и восток.
     */
    private void gable(int x1, int z1, int x2, int z2, int y0, Block stair, Block deck, boolean ridgeAlongZ) {
        int a = ridgeAlongZ ? x1 : z1, b = ridgeAlongZ ? x2 : z2, y = y0;
        int lo = ridgeAlongZ ? z1 : x1, hi = ridgeAlongZ ? z2 : x2;
        while (a <= b) {
            for (int t = lo; t <= hi; t++) {
                if (a == b) {
                    if (ridgeAlongZ) put(a, y, t, deck); else put(t, y, a, deck);
                } else if (ridgeAlongZ) {
                    stairs(a, y, t, stair, Direction.EAST);
                    stairs(b, y, t, stair, Direction.WEST);
                    if (b - a > 1) fill(a + 1, y, t, b - 1, y, t, deck);
                } else {
                    stairs(t, y, a, stair, Direction.SOUTH);
                    stairs(t, y, b, stair, Direction.NORTH);
                    if (b - a > 1) fill(t, y, a + 1, t, y, b - 1, deck);
                }
            }
            a++; b--; y++;
        }
    }

    /** Дом 5x5, 1 или 2 этажа, с лестницей, окнами и светом. */
    private void houseX(int x1, int z1, int x2, int z2, char side, boolean two, Block[] st) {
        int h = two ? 8 : 4;
        building(x1, z1, x2, z2, h, st[0], st[1], st[2], st[3]);
        gable(x1, z1, x2, z2, h, st[4], st[2], side == 'N' || side == 'S');
        int cx = (x1 + x2) / 2, cz = (z1 + z2) / 2;
        switch (side) {
            case 'N' -> doorway(cx, z1);
            case 'S' -> doorway(cx, z2);
            case 'W' -> doorway(x1, cz);
            default -> doorway(x2, cz);
        }
        window(x1, 2, cz); window(x2, 2, cz); window(cx, 2, z1); window(cx, 2, z2);
        put(x1 + 1, 0, z1 + 1, Blocks.OAK_FENCE); put(x1 + 1, 1, z1 + 1, Blocks.LANTERN);
        put(x2 - 1, 0, z1 + 1, Blocks.CRAFTING_TABLE);
        if (two) {
            fill(x1 + 1, 4, z1 + 1, x2 - 1, 4, z2 - 1, st[1]);
            fill(x2 - 1, 0, z2 - 1, x2 - 1, 4, z2 - 1, Blocks.LADDER);
            window(x1, 6, cz); window(x2, 6, cz); window(cx, 6, z1); window(cx, 6, z2);
            put(x1 + 1, 5, z1 + 1, Blocks.OAK_FENCE); put(x1 + 1, 6, z1 + 1, Blocks.LANTERN);
            put(x1 + 1, 5, z2 - 1, Blocks.RED_WOOL);
        }
    }

    private static final int W = 96;        // стена
    private static final int HALF = 100;    // половина стороны территории (200 блоков)

    private void fountain(int x, int z) {
        fill(x - 1, 0, z - 1, x + 1, 0, z + 1, Blocks.STONE_BRICKS);
        put(x, 0, z, Blocks.WATER);
    }

    private void outerCity() {
        Random r = new Random(o.asLong() ^ 0x5DEECE66DL);
        final int[] ringZ = {-72, -40, 40, 72};
        final int[] ringX = {-78, -46, 46, 78};

        // что нельзя застраивать домами и деревьями
        reserve(-31, -29, 41, 29);                                  // старое ядро города
        reserve(-HALF, -3, HALF, 3); reserve(-3, -HALF, 3, HALF);   // главные улицы
        for (int z : ringZ) reserve(-HALF, z - 2, HALF, z + 2);     // кольцевые дороги
        for (int x : ringX) reserve(x - 2, -HALF, x + 2, HALF);
        reserve(49, -HALF, 59, HALF);                               // река
        reserve(W - 2, -HALF, HALF, HALF); reserve(-HALF, -HALF, -(W - 2), HALF);
        reserve(-HALF, W - 2, HALF, HALF); reserve(-HALF, -HALF, HALF, -(W - 2)); // стена
        reserve(60, 44, 76, 68);                                    // парк с прудом
        reserve(-41, 7, -31, 17); reserve(-41, -17, -31, -7); reserve(49, 9, 55, 16); // кузница, пекарня, причал

        // дороги
        fill(-(W + 2), -1, -2, W + 2, -1, 2, Blocks.COBBLESTONE);
        fill(-2, -1, -(W + 2), 2, -1, W + 2, Blocks.COBBLESTONE);
        for (int z : ringZ) fill(-(W - 2), -1, z - 1, W - 2, -1, z + 1, Blocks.GRAVEL);
        for (int x : ringX) fill(x - 1, -1, -(W - 2), x + 1, -1, W - 2, Blocks.GRAVEL);
        fill(-5, -1, -5, 5, -1, 5, Blocks.STONE_BRICKS);            // площадь заново поверх дорог
        fill(-1, 0, -1, 1, 0, 1, Blocks.COBBLESTONE); put(0, -1, 0, Blocks.WATER); put(0, 0, 0, Blocks.AIR);

        // фонтанчики на перекрёстках кольцевых дорог
        for (int x : ringX) for (int z : ringZ) fountain(x, z);

        // река с мостами
        fill(52, -3, -(W - 2), 56, -1, W - 2, Blocks.WATER);
        for (int z : new int[]{0, -40, 40, -72, 72}) {
            int hw = (z == 0) ? 2 : 1;   // главная улица шире кольцевых дорог
            fill(51, -1, z - hw, 57, -1, z + hw, Blocks.STONE_BRICKS);
            fill(52, 0, z - hw - 1, 56, 0, z - hw - 1, Blocks.STONE_BRICK_WALL);
            fill(52, 0, z + hw + 1, 56, 0, z + hw + 1, Blocks.STONE_BRICK_WALL);
        }

        // городская стена: 6 блоков, зубцы, ворота на 4 сторонах
        for (int s : new int[]{-W, W}) {
            fill(-W, 0, s, W, 5, s, Blocks.STONE_BRICKS);
            fill(s, 0, -W, s, 5, W, Blocks.STONE_BRICKS);
            for (int i = -W; i <= W; i += 2) {
                put(i, 6, s, Blocks.STONE_BRICKS); put(s, 6, i, Blocks.STONE_BRICKS);
            }
        }
        fill(-2, 0, -W, 2, 4, -W, Blocks.AIR); fill(-2, 0, W, 2, 4, W, Blocks.AIR);
        fill(-W, 0, -2, -W, 4, 2, Blocks.AIR); fill(W, 0, -2, W, 4, 2, Blocks.AIR);
        for (int sx : new int[]{-W, W}) for (int sz : new int[]{-W, W}) wallTower(sx, sz, 3, 10);
        for (int sg : new int[]{-5, 5}) {
            for (int s : new int[]{-W, W}) { wallTower(sg, s, 2, 9); wallTower(s, sg, 2, 9); }
        }
        for (int s : new int[]{-W, W}) {
            int in = s + (s < 0 ? 3 : -3);
            lamp(-3, in); lamp(3, in); lamp(in, -3); lamp(in, 3);
        }

        // кузница, пекарня и причал
        building(-40, 8, -32, 16, 4, Blocks.COBBLESTONE, Blocks.STONE, Blocks.STONE_BRICKS, Blocks.DARK_OAK_LOG);
        doorway(-32, 12); window(-40, 2, 12); window(-36, 2, 8);
        put(-39, 0, 9, Blocks.FURNACE); put(-38, 0, 9, Blocks.ANVIL); put(-39, 0, 10, Blocks.SMITHING_TABLE); put(-37, 0, 9, Blocks.GRINDSTONE);
        lamp(-31, 10);
        gable(-40, 8, -32, 16, 4, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS, false);
        building(-40, -16, -32, -8, 4, Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.OAK_LOG);
        doorway(-32, -12); window(-40, 2, -12); window(-36, 2, -16);
        put(-39, 0, -15, Blocks.SMOKER); fill(-38, 0, -15, -36, 0, -15, Blocks.BARREL); put(-39, 0, -10, Blocks.HAY_BLOCK);
        lamp(-31, -14);
        gable(-40, -16, -32, -8, 4, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, false);
        fill(50, -1, 12, 54, -1, 13, Blocks.OAK_PLANKS);
        put(54, 0, 12, Blocks.OAK_FENCE); put(54, 1, 12, Blocks.LANTERN); put(51, 0, 14, Blocks.BARREL);

        // парк с прудом
        fill(64, -1, 50, 72, -1, 58, Blocks.WATER);
        fill(60, -1, 47, 76, -1, 47, Blocks.GRAVEL); fill(68, -1, 44, 68, -1, 68, Blocks.GRAVEL);
        put(62, 0, 54, Blocks.OAK_SLAB); put(74, 0, 54, Blocks.OAK_SLAB); put(68, 0, 61, Blocks.OAK_SLAB);
        lamp(60, 45); lamp(76, 45); lamp(60, 67); lamp(76, 67);
        for (int i = 0; i < 16; i++) {
            int tx = 61 + r.nextInt(15), tz = 45 + r.nextInt(22);
            boolean inPond = tx >= 62 && tx <= 74 && tz >= 48 && tz <= 60;
            boolean onPath = tx == 68 || tz == 47;
            if (!inPond && !onPath) tree(tx, tz, 5 + r.nextInt(3));
        }

        // кварталы: клетки 8x8, в каждой дом 5x5 и проулок шириной 3
        for (int gx = -92; gx <= 84; gx += 8) {
            for (int gz = -92; gz <= 84; gz += 8) {
                if (!isFree(gx, gz, gx + 7, gz + 7)) continue;
                fill(gx, -1, gz, gx + 7, -1, gz + 7, Blocks.GRAVEL);
                reserve(gx, gz, gx + 7, gz + 7);
                if (r.nextInt(8) == 0) { tree(gx + 2, gz + 2, 5 + r.nextInt(3)); continue; }
                char[] sides = {'N', 'S', 'E', 'W'};
                houseX(gx, gz, gx + 4, gz + 4, sides[r.nextInt(4)], r.nextInt(3) == 0, STYLES[r.nextInt(STYLES.length)]);
                if (((Math.floorDiv(gx, 8) + Math.floorDiv(gz, 8)) & 1) == 0) lamp(gx + 6, gz + 6);
            }
        }

        // фонари вдоль кольцевых дорог
        for (int i = -90; i <= 90; i += 15) {
            for (int x : ringX) lamp(x + 2, i);
            if (i < 49 || i > 59) for (int z : ringZ) lamp(i, z + 2);
        }

        // цветы на свободной траве
        Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.AZURE_BLUET};
        for (int n = 0; n < 900; n++) {
            int x = r.nextInt(181) - 90, z = r.nextInt(181) - 90;
            if (!isFree(x, z, x, z)) continue;
            if (lv.getBlockState(o.offset(x, -1, z)).is(Blocks.GRASS_BLOCK) && lv.getBlockState(o.offset(x, 0, z)).isAir())
                put(x, 0, z, flowers[r.nextInt(flowers.length)]);
        }
        for (int n = 0; n < 80; n++) {
            int x = 60 + r.nextInt(17), z = 44 + r.nextInt(25);
            boolean pond = x >= 62 && x <= 74 && z >= 48 && z <= 60;
            if (!pond && lv.getBlockState(o.offset(x, -1, z)).is(Blocks.GRASS_BLOCK) && lv.getBlockState(o.offset(x, 0, z)).isAir())
                put(x, 0, z, flowers[r.nextInt(flowers.length)]);
        }

        // деревья в свободных местах
        for (int n = 0, tries = 0; n < 150 && tries < 3000; tries++) {
            int x = r.nextInt(181) - 90, z = r.nextInt(181) - 90;
            if (!isFree(x - 3, z - 3, x + 3, z + 3)) continue;
            tree(x, z, 5 + r.nextInt(3));
            reserve(x - 2, z - 2, x + 2, z + 2);
            n++;
        }
    }

    /** Поселяет жителей: сюжетных по расписанию и горожан для живости. */
    public void spawnNpcs() {
        AABB area = new AABB(o.getX() - 120, o.getY() - 40, o.getZ() - 120, o.getX() + 120, o.getY() + 60, o.getZ() + 120);
        for (Entity old : lv.getEntitiesOfClass(NpcEntity.class, area)) old.discard();

        long t = lv.getDayTime();
        for (NpcDef d : NpcDef.values()) {
            if (d == NpcDef.CITIZEN) continue;
            int[] w = d.waypoint(t);
            spawn(d, o.offset(w[0], 0, w[1]), 0);
        }
        Random r = new Random(o.asLong());
        for (int i = 0; i < 120; i++) {
            int[] s = NpcDef.citizenSpot(t, r);
            spawn(NpcDef.CITIZEN, o.offset(s[0] + r.nextInt(3) - 1, 0, s[1] + r.nextInt(3) - 1), i);
        }
    }

    private void spawn(NpcDef d, BlockPos pos, int seed) {
        NpcEntity e = ModEntities.NPC.get().create(lv);
        if (e == null) return;
        e.setup(d, o, seed);
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        lv.addFreshEntity(e);
    }

    /** Игрок «просыпается» в таверне, на рассвете следующего дня. */
    public void placePlayer(ServerPlayer p) {
        long next = (lv.getDayTime() / 24000L + 1) * 24000L + 200L;
        lv.setDayTime(next);
        p.teleportTo(lv, o.getX() - 13 + 0.5, o.getY(), o.getZ() - 9 + 0.5, 90.0F, 0.0F);
    }
}
