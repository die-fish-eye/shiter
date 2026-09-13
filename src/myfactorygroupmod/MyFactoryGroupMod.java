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

    private void patchFactoryBuilds() {
    int patched = 0;
    for (Block b : Vars.content.blocks()) {
        // === 具体子类优先 ===

        // Fracker（SolidPump 子类）
        if (b instanceof mindustry.world.blocks.production.Fracker fr) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$FrackerBuild")) {
                    b.buildType = () -> new GroupFrackerBuild(fr);
                    patched++;
                    Log.info("[fgm] 已替换 Fracker: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // SolidPump
        if (b instanceof mindustry.world.blocks.production.SolidPump sp) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$SolidPumpBuild")) {
                    b.buildType = () -> new GroupSolidPumpBuild(sp);
                    patched++;
                    Log.info("[fgm] 已替换 SolidPump: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // Pump
        if (b instanceof mindustry.world.blocks.production.Pump p) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$PumpBuild")) {
                    b.buildType = () -> new GroupPumpBuild(p);
                    patched++;
                    Log.info("[fgm] 已替换 Pump: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // Separator
        if (b instanceof mindustry.world.blocks.production.Separator sep) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().equals(
                        "mindustry.world.blocks.production.Separator$SeparatorBuild")) {
                    b.buildType = () -> new GroupSeparatorBuild(sep);
                    patched++;
                    Log.info("[fgm] 已替换 Separator: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // Drill
        if (b instanceof mindustry.world.blocks.production.Drill drill) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$DrillBuild")) {
                    b.buildType = () -> new GroupDrillBuild(drill);
                    patched++;
                    Log.info("[fgm] 已替换 Drill: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // NuclearReactor
        if (b instanceof mindustry.world.blocks.power.NuclearReactor nr) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$NuclearReactorBuild")) {
                    b.buildType = () -> new GroupNuclearReactorBuild(nr);
                    patched++;
                    Log.info("[fgm] 已替换 NuclearReactor: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // VariableReactor
        if (b instanceof mindustry.world.blocks.power.VariableReactor vr) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$VariableReactorBuild")) {
                    b.buildType = () -> new GroupVariableReactorBuild(vr);
                    patched++;
                    Log.info("[fgm] 已替换 VariableReactor: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // ImpactReactor
        if (b instanceof mindustry.world.blocks.power.ImpactReactor ir) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$ImpactReactorBuild")) {
                    b.buildType = () -> new GroupImpactReactorBuild(ir);
                    patched++;
                    Log.info("[fgm] 已替换 ImpactReactor: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // HeaterGenerator
        if (b instanceof mindustry.world.blocks.power.HeaterGenerator hg) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$HeaterGeneratorBuild")) {
                    b.buildType = () -> new GroupHeaterGeneratorBuild(hg);
                    patched++;
                    Log.info("[fgm] 已替换 HeaterGenerator: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // ConsumeGenerator
        if (b instanceof mindustry.world.blocks.power.ConsumeGenerator gen) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$ConsumeGeneratorBuild")) {
                    b.buildType = () -> new GroupConsumeGeneratorBuild(gen);
                    patched++;
                    Log.info("[fgm] 已替换 ConsumeGenerator: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // AttributeCrafter（GenericCrafter 子类）
        if (b instanceof mindustry.world.blocks.production.AttributeCrafter ac) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().endsWith("$AttributeCrafterBuild")) {
                    b.buildType = () -> new GroupAttributeCrafterBuild(ac);
                    patched++;
                    Log.info("[fgm] 已替换 AttributeCrafter: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            continue;
        }

        // GenericCrafter
        if (b instanceof GenericCrafter gc) {
            try {
                Building test = b.buildType.get();
                if (test.getClass().getName().equals(
                        "mindustry.world.blocks.production.GenericCrafter$GenericCrafterBuild")) {
                    b.buildType = () -> new GroupCrafterBuild(gc);
                    patched++;
                    Log.info("[fgm] 已替换 GenericCrafter: @", b.name);
                }
            } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
        }
    }
    Log.info("[fgm] 共替换 @ 个工厂方块", patched);
}
}