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

        // 移除所有旧的 TestFactoryBlock 实例
        List<Block> toRemove = new ArrayList<>();
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) {
                toRemove.add(b);
            }
        }
        for (Block b : toRemove) {
            Log.info("[fgm] 移除旧实例 hash=@", System.identityHashCode(b));
            Vars.content.blocks().remove(b);
        }

        // 添加新方块
        Vars.content.blocks().add(new TestFactoryBlock());

        // 打印每个 TestFactoryBlock 的 hash
        int cnt = 0;
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) {
                Log.info("[fgm] loadContent 中的实例 hash=@ name=@",
                        System.identityHashCode(b), b.name);
                cnt++;
            }
        }
        Log.info("[fgm] loadContent 结束时数量: @", cnt);
    }

    @Override
    public void init() {
        // 去重：如果列表中真的有多个不同实例，移除多余的
        Block first = null;
        List<Block> dupes = new ArrayList<>();
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) {
                if (first == null) {
                    first = b;
                } else if (b != first) {
                    dupes.add(b);
                }
            }
        }
        for (Block b : dupes) {
            Log.info("[fgm] init 移除重复实例 hash=@ name=@",
                    System.identityHashCode(b), b.name);
            Vars.content.blocks().remove(b);
        }

        int finalCnt = 0;
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) finalCnt++;
        }
        Log.info("[fgm] init 结束时数量: @", finalCnt);

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