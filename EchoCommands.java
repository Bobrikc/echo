package com.echocity.velmar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class EchoCommands {
    private EchoCommands() {}

    private static boolean allowed(CommandSourceStack s) {
        return s.hasPermission(2) || s.getServer().isSingleplayer();
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("echocity")
            // выбор варианта ответа (по клику в чате)
            .then(Commands.literal("choose")
                .then(Commands.argument("n", IntegerArgumentType.integer(1, 9))
                    .executes(c -> {
                        StoryEngine.choose(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                        return 1;
                    })))
            // результат мини-игры (win / lose / quit)
            .then(Commands.literal("game")
                .then(Commands.argument("r", StringArgumentType.word())
                    .executes(c -> {
                        StoryEngine.game(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "r"));
                        return 1;
                    })))
            // построить город вокруг игрока и поселить жителей
            .then(Commands.literal("build").requires(EchoCommands::allowed)
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos origin = p.blockPosition();
                    c.getSource().sendSystemMessage(Component.literal("Строю Вельмар... это займёт несколько секунд."));
                    CityBuilder b = new CityBuilder(p.serverLevel(), origin);
                    b.build();
                    b.spawnNpcs();
                    PlayerState.reset(p);
                    PlayerState.setOrigin(p, origin);
                    b.placePlayer(p);
                    Cutscenes.playIntro(p);
                    c.getSource().sendSystemMessage(Component.literal("Вельмар построен. Ты проснулся в таверне «Тихий колокол»: поговори с Тави (ПКМ)."));
                    return 1;
                }))
            // пропустить катсцену (Пробел)
            .then(Commands.literal("skip")
                .executes(c -> {
                    Cutscenes.skip(c.getSource().getPlayerOrException());
                    return 1;
                }))
            // начать историю заново (город остаётся)
            .then(Commands.literal("reset").requires(EchoCommands::allowed)
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean city = PlayerState.hasCity(p);
                    PlayerState.reset(p);
                    p.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.ECHO_SHELL.get()), -1, p.inventoryMenu.getCraftSlots());
                    c.getSource().sendSystemMessage(Component.literal(city
                        ? "История начата заново. Поговори с Тави в таверне."
                        : "Сначала построй город: /echocity build"));
                    return 1;
                }))
            // выдать раковину (если потерялась)
            .then(Commands.literal("shell").requires(EchoCommands::allowed)
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.getInventory().add(new ItemStack(ModItems.ECHO_SHELL.get()));
                    return 1;
                })));
    }
}
