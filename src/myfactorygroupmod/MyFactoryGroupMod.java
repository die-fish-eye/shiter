package myfactorygroupmod;

import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import mindustry.world.Block;

public class MyFactoryGroupMod extends Mod {

    private static TestFactoryBlock testFactory;
    private static boolean registered = false;

    @Override
    public void loadContent() {
        if (registered) return;
        registered = true;

        // 用遍历方式检查是否已存在，最稳
        boolean exists = false;
        for (Block b : Vars.content.blocks()) {
            if ("test-factory".equals(b.name)) {
                exists = true;
                break;
            }
        }
        if (exists) return;

        if (testFactory == null) {
            testFactory = new TestFactoryBlock();
        }
        Vars.content.blocks().add(testFactory);
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

        Timer.schedule(() -> {
            GroupManager.cleanup();

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