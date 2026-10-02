package myfactorygroupmod;

import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;

public class MyFactoryGroupMod extends Mod {

    /** 定时器节拍 = 0.5s；每 DEEP_SWEEP_TICKS 拍无条件做一次全图兜底扫描。 */
    private static final int DEEP_SWEEP_TICKS = 10;
    private static Timer.Task sweepTask;

    /** 是否需要立刻做一次全图兜底扫描（初始化、换图/读档时置位）。 */
    private boolean needsFullSweep = true;
    private int sweepTicks;

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

        // 换图 / 读档：清掉旧地图残留的建筑引用。
        Events.on(WorldLoadEvent.class, e -> {
            GroupManager.reset();
            needsFullSweep = true;
        });

        // 定时器只做两件事：清理死引用（很便宜），以及在需要时兜底全图扫描。
        if (sweepTask != null) sweepTask.cancel();   // 防止 init 被重复调用时定时器叠加
        sweepTask = Timer.schedule(() -> {
            GroupManager.cleanup();

            if (!needsFullSweep && (++sweepTicks % DEEP_SWEEP_TICKS) != 0) return;
            needsFullSweep = false;

            for (Building b : Groups.build) {
                if (b == null || b.block == null) continue;
                if (!b.block.hasItems && !b.block.hasLiquids && !GroupManager.isWall(b)) continue;
                if (!GroupManager.isGroupable(b)) continue;
                FactoryGroup g = GroupManager.getGroup(b);
                if (g == null || GroupManager.hasForeignNeighbor(b)) {
                    GroupManager.onBuildingPlaced(b);
                }
            }
        }, 0.5f, 0.5f);
    }

    @Override
    public void dispose() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
    }

    private void patchFactoryBuilds() {
        int patched = 0;
        for (Block b : Vars.content.blocks()) {

            // === 墙（仅原生 Wall，不含 Door/AutoDoor/ShieldWall 等子类） ===
            // 用 enclosingClass 判断，避免依赖具体内部类名。
            if (b instanceof mindustry.world.blocks.defense.Wall wall) {
                try {
                    Building test = b.buildType.get();
                    Class<?> cls = test.getClass();
                    Class<?> enclosing = cls.getEnclosingClass();

                    boolean isPlainWall =
                        cls.getName().equals("mindustry.world.blocks.defense.Wall$WallBuild")
                        || enclosing == mindustry.world.blocks.defense.Wall.class;

                    if (isPlainWall) {
                        b.buildType = () -> new GroupWallBuild(wall);
                        patched++;
                        Log.info("[fgm] 已替换 墙: @", b.name);
                    }
                } catch (Throwable t) {
                    Log.warn("[fgm] @: @", b.name, t.getMessage());
                }
                continue;
            }

            // === ItemTurret ===
            if (b instanceof mindustry.world.blocks.defense.turrets.ItemTurret it) {
                try {
                    Building test = b.buildType.get();
                    if (test.getClass().getName().endsWith("$ItemTurretBuild")) {
                        b.buildType = () -> new GroupItemTurretBuild(it);
                        it.configurable = true;
                        it.selectionRows = 5;
                        it.selectionColumns = 4;
                        patched++;
                        Log.info("[fgm] 已替换 ItemTurret: @", b.name);
                    }
                } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
                continue;
            }

            // === 工厂子类（具体 → 一般）===

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
        Log.info("[fgm] 共替换 @ 个方块", patched);
    }
}