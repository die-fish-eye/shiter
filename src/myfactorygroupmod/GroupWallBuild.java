package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.blocks.defense.Wall;

public class GroupWallBuild extends Wall.WallBuild {

    public GroupWallBuild(Wall wall) {
        wall.super();
    }

    /** 拦截伤害：所有伤害都从群共享血池扣除，自己返回 0 让原版不再扣血 */
    @Override
    public float handleDamage(float amount) {
        FactoryGroup g = GroupManager.getWallGroup(this);
        if (g == null || g.members.size <= 1) return amount;

        float totalMax = 0f;
        for (Building b : g.members) totalMax += b.maxHealth;
        if (totalMax <= 0f) return 0f;

        float oldFrac = g.wallHealthFraction;
        float newFrac = Math.max(0f, oldFrac - amount / totalMax);
        g.wallHealthFraction = newFrac;

        // 同步其他成员血量
        for (Building b : g.members) {
            if (b != this) b.health = b.maxHealth * newFrac;
        }

        // 同步自己，返回 0 表示原版不再额外扣血
        health = maxHealth * newFrac;

        // 群血量归零：所有墙一起倒
        if (newFrac <= 0f) {
            Seq<Building> snapshot = g.members.toSeq();
            for (Building b : snapshot) {
                if (b.isValid() && !b.dead()) {
                    b.health = 0f;
                    Call.buildDestroyed((Building) b);
                }
            }
        }

        return 0f;
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}