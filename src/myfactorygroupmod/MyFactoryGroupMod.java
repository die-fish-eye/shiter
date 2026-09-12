package myfactorygroupmod;

import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import mindustry.world.Block;

public class MyFactoryGroupMod extends Mod {

    @Override
    public void loadContent() {
        Log.info("[fgm] loadContent 开始，当前方块总数: @", Vars.content.blocks().size);

        // 移除所有旧的 TestFactoryBlock 实例（无论什么名字）
        List<Block> toRemove = new ArrayList<>();
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) {
                toRemove.add(b);
            }
        }
        for (Block b : toRemove) {
            Log.info("[fgm] loadContent 移除旧实例: @", b.name);
            Vars.content.blocks().remove(b);
        }

        TestFactoryBlock block = new TestFactoryBlock();
        Vars.content.blocks().add(block);

        int cnt = 0;
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) cnt++;
        }
        Log.info("[fgm] loadContent 结束时 TestFactoryBlock 数量: @", cnt);
    }

    @Override
    public void init() {
        int cnt = 0;
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) {
                cnt++;
                Log.info("[fgm] init 中找到: @", b.name);
            }
        }
        Log.info("[fgm] init 时 TestFactoryBlock 数量: @", cnt);

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