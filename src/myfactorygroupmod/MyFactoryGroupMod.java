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

    private static final String BLOCK_NAME = "test-factory";

    @Override
    public void loadContent() {
        Log.info("[factory-group-mod] loadContent 被调用");

        // 遍历检查是否已有同名方块
        boolean exists = false;
        int sameNameCount = 0;
        for (Block b : Vars.content.blocks()) {
            if (b.name != null && b.name.equals(BLOCK_NAME)) {
                sameNameCount++;
                exists = true;
            }
        }
        Log.info("[factory-group-mod] 注册前，同名方块数量: @", sameNameCount);

        if (exists) return;

        Vars.content.blocks().add(new TestFactoryBlock());
    }

    @Override
    public void init() {
        // 去重：移除所有同名方块中的多余项，只保留第一个
        dedupe();

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

    private void dedupe() {
        List<Block> duplicates = new ArrayList<>();
        boolean firstFound = false;
        int count = 0;
        for (Block b : Vars.content.blocks()) {
            if (b.name != null && b.name.equals(BLOCK_NAME)) {
                count++;
                if (!firstFound) {
                    firstFound = true;
                } else {
                    duplicates.add(b);
                }
            }
        }
        Log.info("[factory-group-mod] init 时同名方块数量: @", count);
        for (Block b : duplicates) {
            Log.info("[factory-group-mod] 移除重复方块: @", b);
            b.remove();
        }
    }
}