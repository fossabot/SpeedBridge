package me.tofpu.speedbridge.command.sub;

import co.aikar.commands.annotation.*;
import me.tofpu.speedbridge.api.game.GameService;
import me.tofpu.speedbridge.api.game.Result;
import me.tofpu.speedbridge.api.island.mode.Mode;
import me.tofpu.speedbridge.api.lobby.LobbyService;
import me.tofpu.speedbridge.api.user.User;
import me.tofpu.speedbridge.api.user.UserProperties;
import me.tofpu.speedbridge.api.user.UserService;
import me.tofpu.speedbridge.command.BridgeBaseCommand;
import me.tofpu.speedbridge.data.file.path.Path;
import me.tofpu.speedbridge.island.mode.ModeManager;
import me.tofpu.speedbridge.util.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("speedbridge|game")
public class MainCommand extends BridgeBaseCommand {

    private final UserService userService;
    private final GameService gameService;
    private final LobbyService lobbyService;

    public MainCommand(final UserService userService, final GameService gameService, final LobbyService lobbyService) {
        super("game");
        this.userService = userService;
        this.gameService = gameService;
        this.lobbyService = lobbyService;
    }

    @Override
    @Private
    @Subcommand("help")
    @Description("Shows you all the available commands")
    public void onHelp(final CommandSender sender) {
        super.onHelp(sender);
    }

    @Override
    @Private
    @CatchUnknown
    public void onUnknownCommand(final CommandSender sender) {
        super.onUnknownCommand(sender);
    }

    @Subcommand("join")
    @CommandAlias("join")
    @Syntax("[mode]|[slot]")
    @CommandCompletion("@modes|@availableIslands")
    @Description("Joins a practice island")
    public void onJoin(final Player player, @Optional String arg) {
        Integer integer = Util.parseInt(arg);
        Mode mode = null;
        if (integer == null) {
            mode = ModeManager.getModeManager().get(arg);
        }
        onJoin(player, integer, mode);
    }

    @Subcommand("leave")
    @CommandAlias("leave")
    @Description("Leaves the practice island")
    public void onLeave(final Player player) {
        if (!gameService.isPlaying(player)) {
            Util.message(player, Path.MESSAGES_NOT_PLAYING);
            return;
        }
        gameService.leave(player);
    }

    @Subcommand("leaderboard")
    @CommandAlias("leaderboard")
    @Description("Lists the top 10 best performers")
    public void onLeaderboard(final CommandSender player) {
        player.sendMessage(lobbyService.getLeaderboard().print());
    }

    @Subcommand("score")
    @CommandAlias("score")
    @Description("Your personal best score")
    public void onScore(final Player player) {
        final User user = userService.get(player.getUniqueId());
        final UserProperties properties = user == null ? null : user.properties();

        double score = user == null ? 0 : properties.timer() == null ? 0 : properties.timer().result();
        Util.message(player, Path.MESSAGES_YOUR_SCORE, new String[]{"%score%"}, score + "");
    }

    @Subcommand("lobby")
    @CommandAlias("lobby")
    @Description("Teleports you to the lobby")
    public void onLobby(final Player player) {
        if (gameService.isPlaying(player)) {
            gameService.leave(player);
        }
        if (lobbyService.hasLobbyLocation()) player.teleport(lobbyService.getLobbyLocation());
    }

    private void onJoin(final Player player, final Integer integer, final Mode mode) {
        final Result result;
        if (integer != null) {
            result = gameService.join(player, integer);
        } else if (mode != null) {
            result = gameService.join(player, mode);
        } else result = gameService.join(player);

        final Path.Value<?> path;
        switch (result) {
            case FAIL:
                path = Path.MESSAGES_ALREADY_JOINED;
                break;
            case FULL:
            case INVALID_ISLAND:
                path = Path.MESSAGES_NOT_AVAILABLE;
                break;
            case NONE:
                path = Path.MESSAGES_NO_AVAILABLE;
                break;
            case SUCCESS:
                path = Path.MESSAGES_JOINED;
                break;
            case INVALID_LOBBY:
                if (player.isOp()) {
                    path = Path.MESSAGES_NO_LOBBY;
                    break;
                } else path = Path.MESSAGES_NO_AVAILABLE;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + result);
        }

        Util.message(player, path);
    }
}
