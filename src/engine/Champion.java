package engine;

public class Champion {
    private final String id;
    private final String teamSide;
    private final int cost;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int range;
    private final int moveRange;
    private final int speed;
    private final int maxMana;
    private final int skillManaCost;
    private final int skillCooldown;

    private int hp;
    private int mana;
    private int remainingCooldown;
    private boolean alive;
    private Position position;

    public Champion(String id, String teamSide, int cost, int maxHp,
                    int attack, int defense, int range, int moveRange,
                    int speed, int maxMana, int skillManaCost, int skillCooldown) {
        this.id = id;
        this.teamSide = teamSide;
        this.cost = cost;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.range = range;
        this.moveRange = moveRange;
        this.speed = speed;
        this.maxMana = maxMana;
        this.skillManaCost = skillManaCost;
        this.skillCooldown = skillCooldown;
        this.hp = maxHp;
        this.mana = 0;
        this.remainingCooldown = 0;
        this.alive = true;
    }

    public void takeDamage(int dmg) {
        int actual = Math.max(1, dmg - this.defense);
        this.hp = Math.max(0, this.hp - actual);
        if (this.hp == 0) this.alive = false;
    }

    public void gainMana(int amount) {
        this.mana = Math.min(maxMana, this.mana + amount);
    }

    public void heal(int amount) {
        this.hp = Math.min(maxHp, this.hp + amount);
    }

    public void tickCooldown() {
        if (remainingCooldown > 0) remainingCooldown--;
    }

    public void useSkill() {
        this.mana -= skillManaCost;
        this.remainingCooldown = skillCooldown;
    }

    public String getId()               { return id; }
    public String getTeamSide()         { return teamSide; }
    public int getCost()                { return cost; }
    public int getHp()                  { return hp; }
    public int getMaxHp()               { return maxHp; }
    public int getAttack()              { return attack; }
    public int getDefense()             { return defense; }
    public int getRange()               { return range; }
    public int getMoveRange()           { return moveRange; }
    public int getSpeed()               { return speed; }
    public int getMana()                { return mana; }
    public int getMaxMana()             { return maxMana; }
    public int getSkillManaCost()       { return skillManaCost; }
    public int getRemainingCooldown()   { return remainingCooldown; }
    public boolean isAlive()            { return alive; }
    public Position getPosition()       { return position; }
    public void setPosition(Position p) { this.position = p; }

    @Override
    public String toString() {
        return id + "[" + teamSide + "] HP:" + hp + "/" + maxHp;
    }
}