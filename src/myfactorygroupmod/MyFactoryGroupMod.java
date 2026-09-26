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
        Log.info("[fgm] loadContent done (no custom blocks)");
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
                if (b == null || b.block == null) continue;
                if (!GroupManager.isGroupable(b)) continue;
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

            // === Wall: dual-condition match, with diagnostics ===
            if (b instanceof mindustry.world.blocks.defense.Wall wall) {
                try {
                    Building test = b.buildType.get();
                    Class<?> cls = test.getClass();
                    Class<?> enclosing = cls.getEnclosingClass();
                    String clsName = cls.getName();
                    String encName = enclosing == null ? "null" : enclosing.getName();

                    Log.info("[fgm] Wall candidate: @ cls=@ enc=@",
                        b.name, clsName, encName);

                    boolean isPlainWall =
                        clsName.equals("mindustry.world.blocks.defense.Wall$WallBuild")
                        || enclosing == mindustry.world.blocks.defense.Wall.class;

                    if (isPlainWall) {
                        b.buildType = () -> new GroupWallBuild(wall);
                        patched++;
                        Log.info("[fgm] replaced Wall: @", b.name);
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
                        Log.info("[fgm] replaced ItemTurret: @", b.name);
                    }
                } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
                continue;
            }

            // === factory subclasses ===
            if (b instanceof mindustry.world.blocks.production.Fracker fr) {
                try {
                    Building test = b.buildType.get();
                    if (test.getClass().getName().endsWith("$FrackerBuild")) {
                        b.buildType = () -> new GroupFrackerBuild(fr);
                        patched++;
                        Log.info("[fgm] replaced Fracker: @", b.name);
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
                        Log.info("[fgm] replaced SolidPump: @", b.name);
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
                        Log.info("[fgm] replaced Pump: @", b.name);
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
                        Log.info("[fgm] replaced Separator: @", b.name);
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
                        Log.info("[fgm] replaced Drill: @", b.name);
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
                        Log.info("[fgm] replaced NuclearReactor: @", b.name);
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
                        Log.info("[fgm] replaced VariableReactor: @", b.name);
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
                        Log.info("[fgm] replaced ImpactReactor: @", b.name);
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
                        Log.info("[fgm] replaced HeaterGenerator: @", b.name);
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
                        Log.info("[fgm] replaced ConsumeGenerator: @", b.name);
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
                        Log.info("[fgm] replaced AttributeCrafter: @", b.name);
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
                        Log.info("[fgm] replaced GenericCrafter: @", b.name);
                    }
                } catch (Throwable t) { Log.warn("[fgm] @: @", b.name, t.getMessage()); }
            }
        }
        Log.info("[fgm] replaced @ blocks total", patched);
    }
}