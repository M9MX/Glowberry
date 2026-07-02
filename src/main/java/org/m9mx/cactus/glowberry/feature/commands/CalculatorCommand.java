package org.m9mx.cactus.glowberry.feature.commands;

import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.util.game.ChatUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.m9mx.cactus.glowberry.util.calc.CalcUtil;
import org.m9mx.cactus.glowberry.util.calc.CalculatorSession;
import org.m9mx.cactus.glowberry.util.calc.ExpressionEvaluator;

import java.util.List;

/**
 * A full in-chat calculator command for the Glowberry Cactus addon.
 *
 * Usage (invoked with the Cactus prefix, e.g. {@code #calc}):
 *   - {@code #calc <expression>}                       evaluate a math expression
 *   - {@code #calc convert <to|from> <overworld|nether> <x> <z>}  convert coordinates
 *   - {@code #calc breakdown <count>}                  break an item count into shulkers/stacks/items
 *   - {@code #calc history}                            print this session's calculation history
 *   - {@code #calc clear}                              reset history and 'ans'
 *   - {@code #calc usage}                              show all available features
 *
 * This class is intentionally thin: all the working logic lives in
 * {@code org.m9mx.cactus.glowberry.util.calc}.
 */
public class CalculatorCommand extends Command {

    private static final String PREFIX = "Calc";

    public CalculatorCommand() {
        this("calc");
    }

    public CalculatorCommand(String name) {
        super(name);
    }

    @Override
    public void build(LiteralArgumentBuilder builder) {
        // #calc history
        builder.then(literal("history").executes(context -> {
            printHistory();
            return SINGLE_SUCCESS;
        }));

        // #calc clear
        builder.then(literal("clear").executes(context -> {
            CalculatorSession.clear();
            ChatUtils.infoPrefix(PREFIX, "History and 'ans' cleared.");
            return SINGLE_SUCCESS;
        }));

        // #calc usage
        builder.then(literal("usage").executes(context -> {
            printUsage();
            return SINGLE_SUCCESS;
        }));

        // #calc breakdown <count>
        builder.then(literal("breakdown")
                .then(argument("count", LongArgumentType.longArg(0)).executes(context -> {
                    long count = LongArgumentType.getLong(context, "count");
                    printBreakdown(count);
                    return SINGLE_SUCCESS;
                })));

        // #calc convert <to|from> <overworld|nether> <x> <z>
        //   to overworld   / from nether   -> Nether -> Overworld (x8)
        //   to nether      / from overworld -> Overworld -> Nether (/8)
        //   from overworld / from nether (no args) -> uses player's current position
        builder.then(literal("convert")
                .then(literal("to")
                        .then(convertLeaf("overworld", true))
                        .then(convertLeaf("nether", false)))
                .then(literal("from")
                        .then(fromConvertLeaf("overworld", false))
                        .then(fromConvertLeaf("nether", true))));

        // #calc <expression>  (greedy; must be registered after literals so subcommands win)
        builder.then(argument("expression", StringArgumentType.greedyString()).executes(context -> {
            evaluateExpression(StringArgumentType.getString(context, "expression"));
            return SINGLE_SUCCESS;
        }));

        // bare #calc -> usage help
        builder.executes(context -> {
            printUsage();
            return SINGLE_SUCCESS;
        });
    }

    /**
     * Builds a {@code <overworld|nether> <x> <z>} leaf for the convert command.
     *
     * @param dimension        the dimension literal name
     * @param netherToOverworld whether this leaf converts Nether coords to Overworld (x8) or the reverse (/8)
     */
    private LiteralArgumentBuilder convertLeaf(String dimension, boolean netherToOverworld) {
        return literal(dimension)
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double x = DoubleArgumentType.getDouble(context, "x");
                                    double z = DoubleArgumentType.getDouble(context, "z");
                                    printConvert(x, z, netherToOverworld);
                                    return SINGLE_SUCCESS;
                                })));
    }

    /**
     * Builds a {@code <overworld|nether>} leaf for the {@code from} subcommand that
     * also provides a zero-argument fallback using {@code .executes(...)}. The
     * fallback captures the player's current position, verifies they are actually
     * in the expected dimension, and performs the conversion with a {@code Current} label.
     */
    private LiteralArgumentBuilder fromConvertLeaf(String dimension, boolean netherToOverworld) {
        return literal(dimension)
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double x = DoubleArgumentType.getDouble(context, "x");
                                    double z = DoubleArgumentType.getDouble(context, "z");
                                    printConvert(x, z, netherToOverworld);
                                    return SINGLE_SUCCESS;
                                })))
                .executes(context -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.level == null) {
                        return SINGLE_SUCCESS;
                    }

                    double x = mc.player.getX();
                    double z = mc.player.getZ();

                    ResourceKey<Level> currentDim = mc.level.dimension();
                    ResourceKey<Level> expectedDim = dimension.equals("overworld") ? Level.OVERWORLD : Level.NETHER;

                    if (!currentDim.equals(expectedDim)) {
                        String dimDisplay = dimension.equals("overworld") ? "Overworld" : "Nether";
                        ChatUtils.errorPrefix(PREFIX, "§cYou are not in the " + dimDisplay + " to use your current location!");
                    } else {
                        printConvertWithCurrent(x, z, netherToOverworld);
                    }

                    return SINGLE_SUCCESS;
                });
    }

    /**
     * Same output as {@link #printConvert(double, double, boolean)} but labels the
     * source coordinates as {@code §eCurrent§7} instead of just showing the raw numbers.
     */
    private void printConvertWithCurrent(double x, double z, boolean netherToOverworld) {
        CalcUtil.CoordResult result = netherToOverworld
                ? CalcUtil.netherToOverworld(x, z)
                : CalcUtil.overworldToNether(x, z);
        String from = netherToOverworld ? "Nether" : "Overworld";
        String to = netherToOverworld ? "Overworld" : "Nether";
        ChatUtils.infoPrefix(PREFIX, "§7" + from + " §eCurrent§7 (§f" + CalcUtil.formatCoord(x) + "§7, §f" + CalcUtil.formatCoord(z)
                + "§7) §6->§7 " + to + " (§a§l" + CalcUtil.formatCoord(result.x) + "§r§7, §a§l"
                + CalcUtil.formatCoord(result.z) + "§r§7)");
    }

    private void evaluateExpression(String expression) {
        try {
            ExpressionEvaluator.EvalResult result = ExpressionEvaluator.evaluateDetailed(expression, CalculatorSession.getAns());
            CalculatorSession.record(expression, result.value);
            ChatUtils.infoPrefix(PREFIX, result.display + " §7= §a§l" + CalcUtil.formatNumber(result.value));
        } catch (ExpressionEvaluator.EvaluationException e) {
            ChatUtils.errorPrefix(PREFIX, "§c" + e.getMessage());
        } catch (Exception e) {
            // Catch-all fallback for any unexpected evaluation anomaly.
            ChatUtils.errorPrefix(PREFIX, "§cCould not evaluate expression.");
        }
    }

    private void printConvert(double x, double z, boolean netherToOverworld) {
        CalcUtil.CoordResult result = netherToOverworld
                ? CalcUtil.netherToOverworld(x, z)
                : CalcUtil.overworldToNether(x, z);
        String from = netherToOverworld ? "Nether" : "Overworld";
        String to = netherToOverworld ? "Overworld" : "Nether";
        ChatUtils.infoPrefix(PREFIX, "§7" + from + " (§f" + CalcUtil.formatCoord(x) + "§7, §f" + CalcUtil.formatCoord(z)
                + "§7) §6->§7 " + to + " (§a§l" + CalcUtil.formatCoord(result.x) + "§r§7, §a§l"
                + CalcUtil.formatCoord(result.z) + "§r§7)");
    }

    private void printBreakdown(long count) {
        CalcUtil.StorageBreakdown b = CalcUtil.breakdown(count);
        ChatUtils.infoPrefix(PREFIX, "§f" + b.total + " §7items = §a§l" + b.shulkers + "§r§7 shulker(s), §a§l"
                + b.stacks + "§r§7 stack(s), §a§l" + b.items + "§r§7 item(s)");
    }

    private void printHistory() {
        List<CalculatorSession.Entry> history = CalculatorSession.getHistory();
        if (history.isEmpty()) {
            ChatUtils.infoPrefix(PREFIX, "No calculations yet this session.");
            return;
        }
        ChatUtils.infoPrefix(PREFIX, "§6Session history §7(" + history.size() + "):");
        int index = 1;
        for (CalculatorSession.Entry entry : history) {
            ChatUtils.info("§7" + index + ". §f" + entry.expression().trim() + " §7= §a" + CalcUtil.formatNumber(entry.result()));
            index++;
        }
    }

    private void printUsage() {
        ChatUtils.infoPrefix(PREFIX, "§6§lCalculator §7- usage:");
        ChatUtils.info("§a<expression> §7- math: §f+ - * / % ^§7 | §fpow power root sqrt§7 | §f( )");
        ChatUtils.info("§7  vars: §bstack§7(64) §bshulker§7(1728) §bdouble§7(3456) §bans §bpi §be");
        ChatUtils.info("§7  e.g. §f(2 ^ 3) + root 16§7, §fans + stack§7, §f5 * shulker");
        ChatUtils.info("§aconvert <to|from> <overworld|nether> <x> <z>§7 - convert coords between dimensions");
        ChatUtils.info("§aconvert from <overworld|nether>§7 - convert your current position (dimension-guarded)");
        ChatUtils.info("§abreakdown <count>§7 - split an item count into shulkers/stacks/items");
        ChatUtils.info("§ahistory§7 - show this session's calculations");
        ChatUtils.info("§aclear§7 - reset history and 'ans'");
        ChatUtils.info("§ausage§7 - show this help");
    }
}
