package game.objectives;

// forme degli obiettivi: per ogni esagono, il percorso di direzioni dall'ancoraggio (come koiShape in Pond)
public final class ObjectiveShapes {
    public static final int[][] SHISHI_ODOSHI = {{}, {0}, {1}};
    public static final int[][] OLD_CARP      = {{}, {4}, {5}, {1}};
    public static final int[][] SMALL_BRANCH  = {{}, {2}, {2, 2}};
    public static final int[][] MEDIUM_BRANCH = {{}, {2}, {4}, {0}};
    public static final int[][] BIG_BRANCH    = {{}, {1}, {3}, {0}, {4}};
    public static final int[][] THREE         = {{}, {1}, {0}, {1,0}};

    private ObjectiveShapes() {
    }
}
