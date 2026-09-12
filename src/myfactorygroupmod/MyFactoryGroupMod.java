package myfactorygroupmod;

import arc.Events;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.mod.Mod;

public class MyFactoryGroupMod extends Mod {

    @Override
    public void init() {
        // 监听方块放置/拆除事件
        Events.on(BlockBuildEndEvent.class, e -> {
            if (e.tile == null || e.tile.build == null) return;
            if (e.breaking) {
                GroupManager.onBuildingRemoved(e.tile.build);
            } else {
                GroupManager.onBuildingPlaced(e.tile.build);
            }
        });

        // 注册 UI 对话框
        GroupInfoDialog.register();
    }
}