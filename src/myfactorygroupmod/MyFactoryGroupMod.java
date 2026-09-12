package myfactorygroupmod;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.mod.Mod;

public class MyFactoryGroupMod extends Mod {

    @Override
    public void loadContent() {
        // 注册自定义测试工厂方块
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

        // UI 注册（现在只做占位，实际展示已经移到悬停面板）
        // GroupInfoDialog.register();
    }
}