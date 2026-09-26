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

    /** 把比例同步到所有成员 */
    private void syncHealth(FactoryGroup g, float frac) {
        g.wallHealthFraction = frac;
        for (Building b : g.members) {
            b.health = b.maxHealth * frac;
        }
    }

    private float totalMax(FactoryGroup g) {
        float t = 0f;
        for (Building b : g.members) t += b.maxHealth;
        return t;
    }

    /** 拦截伤害：从群共享血池扣，返回 0 让原版不再扣自己的血 */
    @Override
    public float handleDamage(float amount) {
        FactoryGroup g = GroupManager.getWallGroup(this);
        if (g == null || g.members.size <= 1) return amount;

        float tm = totalMax(g);
        if (tm <= 0f) return 0f;

        float newFrac = Math.max(0f, g.wallHealthFraction - amount / tm);
        syncHealth(g, newFrac);

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

    /** 治疗也走群共享血池 */
    @Override
    public void heal(float amount) {
        FactoryGroup g = GroupManager.getWallGroup(this);
        if (g == null || g.members.size <= 1) {
            super.heal(amount);
            return;
        }
        float tm = totalMax(g);
        if (tm <= 0f) return;

        float newFrac = Math.min(1f, g.wallHealthFraction + amount / tm);
        syncHealth(g, newFrac);
    }

    /** 无参 heal()（全恢复）也走群血池 */
    @Override
    public void heal() {
        FactoryGroup g = GroupManager.getWallGroup(this);
        if (g == null || g.members.size <= 1) {
            super.heal();
            return;
        }
        syncHealth(g, 1f);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}