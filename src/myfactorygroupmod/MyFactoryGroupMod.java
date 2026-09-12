package myfactorygroupmod;

import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;

public class MyFactoryGroupMod extends Mod {

    /** 静态单例，防止重复创建 */
    private static TestFactoryBlock testFactory;

    @Override
    public void loadContent() {
        // 如果已存在同名方块，就不重复注册
        if (Vars.content.block("test-factory") != null) return;

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