package game;

import java.util.List;
import java.util.function.IntConsumer;

public record PendingChoice(Player player, String question, List<String> options, IntConsumer onAnswer) {}
