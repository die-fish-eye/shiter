package myfactorygroupmod;

import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;

public class MyFactoryGroupMod extends Mod {

    @Override
    public void loadContent() {
        // 不注册任何自定义方块，避免与 MindustryX / MI2-Utilities 冲突
        Log.info("[fgm] loadContent 完成（不添加自定义方块）");
    }

    @Override
    public void init() {
        Events.on(BlockBuildEndEvent.class, e -> {
            if (e.tile == null || e.tile.build == null) return;

            if (e.breaking) {
                GroupManager.onBuildingRemoved(e.tile.build);
                return;
            }

            Building b = e.tile.build;
            if (b.block == null || !b.block.hasItems) return;

            GroupManager.onBuildingPlaced(b);

            // 放置后立即提示群信息
            FactoryGroup g = GroupManager.getGroup(b);
            if (g != null && g.members.size > 1) {
                Vars.ui.hudfrag.showToast(
                    "[accent]工厂群：[]" + g.members.size + " 个工厂"
                );
            }
        });

        // 兜底：清理失效 + 分配遗漏
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