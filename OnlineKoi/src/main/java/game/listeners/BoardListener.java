package game.listeners;

public interface BoardListener {
    void playerHasToPay(int cost);
    void playerGiveBonusMoneyWater(int secondBonusMoney, int secondBonusWater);
    void playerAskBonusOrMarket(int thirdBonusMoney, int thirdBonusWater);
    void playerAskBorderOrLeaf(int fifthBonusBorder, int fifthBonusLeaf);
    void noMoreCards();
}
