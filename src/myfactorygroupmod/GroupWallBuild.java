package myfactorygroupmod;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.blocks.defense.Wall;

public class GroupWallBuild extends Wall.WallBuild {

    public GroupWallBuild(Wall wall) {
        wall.super();
    }

    /** hook into vanilla damage pipeline: return value is subtracted from this wall's health */
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

        // sync other members
        for (Building b : g.members) {
            if (b != this) b.health = b.maxHealth * newFrac;
        }

        // sync self, then return 0 so vanilla doesn't subtract again
        health = maxHealth * newFrac;

        // group wiped: bring down every wall at once
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
}