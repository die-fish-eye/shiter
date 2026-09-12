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
        // 移除旧的
        List<Block> old = new ArrayList<>();
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) old.add(b);
        }
        for (Block b : old) Vars.content.blocks().remove(b);

        int before = Vars.content.blocks().size;
        Vars.content.blocks().add(new TestFactoryBlock());
        int after = Vars.content.blocks().size;

        Log.info("[fgm] add 前=@ add 后=@ 差值=@", before, after, after - before);
    }

    @Override
    public void init() {
        // 反复移除，直到只剩下 1 个
        for (int round = 0; round < 5; round++) {
            int cnt = 0;
            Block first = null;
            for (Block b : Vars.content.blocks()) {
                if (b instanceof TestFactoryBlock) {
                    cnt++;
                    if (first == null) first = b;
                }
            }
            Log.info("[fgm] init 第 @ 轮，count=@", round, cnt);
            if (cnt <= 1) break;
            Vars.content.blocks().remove(first);
        }

        int finalCnt = 0;
        for (Block b : Vars.content.blocks()) {
            if (b instanceof TestFactoryBlock) finalCnt++;
        }
        Log.info("[fgm] init 最终 count=@", finalCnt);

        Events.on(BlockBuildEndEvent.class, e -> {
            if (e.tile == null || e.tile.build == null) return;
            if (e.breaking) GroupManager.onBuildingRemoved(e.tile.build);
            else GroupManager.onBuildingPlaced(e.tile.build);
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