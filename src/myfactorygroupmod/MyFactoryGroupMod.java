package myfactorygroupmod;

import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;

public class MyFactoryGroupMod extends Mod {

    @Override
    public void loadContent() {
        Log.info("[fgm] loadContent 完成（不新增方块）");
    }

    @Override
    public void init() {
        patchFactoryBuilds();

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

    /** 把原版工厂方块的 buildType 替换为 GroupCrafterBuild */
    private void patchFactoryBuilds() {
        int patched = 0;
        for (Block b : Vars.content.blocks()) {
            if (!(b instanceof GenericCrafter gc)) continue;
            try {
                Building test = b.buildType.get();
                String cls = test.getClass().getName();
                // 只替换原生的 GenericCrafterBuild，跳过独立子类（Separator 等）
                if (cls.equals("mindustry.world.blocks.production.GenericCrafter$GenericCrafterBuild")) {
                    b.buildType = () -> new GroupCrafterBuild(gc);
                    patched++;
                    Log.info("[fgm] 已替换: @", b.name);
                }
            } catch (Throwable t) {
                Log.warn("[fgm] 检查 @ 时出错: @", b.name, t.getMessage());
            }
        }
        Log.info("[fgm] 共替换 @ 个工厂方块", patched);
    }
}