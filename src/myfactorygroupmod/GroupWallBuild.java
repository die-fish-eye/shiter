package myfactorygroupmod;

import arc.math.Mathf;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.blocks.defense.Wall;

public class GroupWallBuild extends Wall.WallBuild {

    public GroupWallBuild(Wall wall) {
        wall.super();
    }

    /** vanilla would deduct from local health; we route all damage through the group pool */
    @Override
    public float handleDamage(float amount) {
        FactoryGroup g = GroupManager.getWallGroup(this);
        if (g == null || g.members.size <= 1) return amount;
        return 0f;
    }

    @Override
    public void damage(float amount) {
        if (dead()) return;

        FactoryGroup g = GroupManager.getWallGroup(this);
        if (g == null || g.members.size <= 1) {
            super.damage(amount);
            return;
        }

        // apply vanilla block-health rule
        float dm = Vars.state.rules.blockHealth(team);
        if (Mathf.zero(dm)) {
            g.wallHealthFraction = 0f;
        } else {
            amount /= dm;
        }

        // total max hp of the group
        float totalMax = 0f;
        for (Building b : g.members) totalMax += b.maxHealth;
        if (totalMax <= 0f) return;

        // deduct from group pool
        g.wallHealthFraction = Math.max(0f, g.wallHealthFraction - amount / totalMax);

        // sync each member's health
        for (Building b : g.members) {
            b.health = b.maxHealth * g.wallHealthFraction;
        }

        // if group pool is empty, kill every wall at once
        if (g.wallHealthFraction <= 0f) {
            arc.struct.Seq<Building> snapshot = g.members.toSeq();
            for (Building b : snapshot) {
                if (b.isValid() && !b.dead()) {
                    b.health = 0f;
                    Call.buildDestroyed((Building) b);
                }
            }
        }
    }
}