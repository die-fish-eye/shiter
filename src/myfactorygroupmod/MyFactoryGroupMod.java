package myfactorygroupmod;

import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;

public class MyFactoryGroupMod extends Mod {

    @Override
    public void loadContent() {
        Vars.content.blocks().add(new TestFactoryBlock());
    }

    @Override
    public void init() {
        Events.on(BlockBuildEndEvent.class, e -> {
            if (e.tile == null || e.tile.build == null) return;
            if (e.breaking) {
                GroupManager.onBuildingRemoved(e.tile.build);
            } else {
                GroupManager.onBuildingPlaced(e.tile.build);
            }
        });

        // 每 0.5 秒检查：没群的，或与邻居不同群的，重新分配
        Timer.schedule(() -> {
            for (Building b : Groups.build) {
                if (b == null || b.block == null || !b.block.hasItems) continue;
                FactoryGroup g = GroupManager.getGroup(b);
                if (g == null || GroupManager.hasForeignNeighbor(b)) {
                    GroupManager.onBuildingPlaced(b);
                }
            }
        }, 0.5f, 0.5f);
    }
}