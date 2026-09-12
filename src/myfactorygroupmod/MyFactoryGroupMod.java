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
        // 主路径：放置/拆除事件
        Events.on(BlockBuildEndEvent.class, e -> {
            if (e.tile == null || e.tile.build == null) return;
            if (e.breaking) {
                GroupManager.onBuildingRemoved(e.tile.build);
            } else {
                GroupManager.onBuildingPlaced(e.tile.build);
            }
        });

        // 兜底：每 0.5 秒扫一遍所有建筑，把遗漏的 hasItems 建筑登记进群
        // 这样即使 MindustryX 不触发 BlockBuildEndEvent 也能工作
        Timer.schedule(() -> {
            for (Building b : Groups.build) {
                if (b == null || b.block == null) continue;
                if (!b.block.hasItems) continue;
                if (GroupManager.getGroup(b) != null) continue;
                GroupManager.onBuildingPlaced(b);
            }
        }, 0.5f, 0.5f);
    }
}