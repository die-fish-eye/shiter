package myfactorygroupmod;

import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import java.lang.reflect.Field;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.blocks.power.HeaterGenerator;
import mindustry.world.blocks.power.ImpactReactor;
import mindustry.world.blocks.power.NuclearReactor;
import mindustry.world.blocks.power.VariableReactor;

/**
 * Power-type Group*Build classes.
 *  - GroupConsumeGeneratorBuild
 *  - GroupHeaterGeneratorBuild
 *  - GroupImpactReactorBuild
 *  - GroupVariableReactorBuild
 *  - GroupNuclearReactorBuild
 */
public final class GroupPowerBuilds {
    private GroupPowerBuilds() {}
}

// ============================================================
// ConsumeGenerator
// ============================================================
class GroupConsumeGeneratorBuild extends ConsumeGenerator.ConsumeGeneratorBuild {

    public GroupConsumeGeneratorBuild(ConsumeGenerator gen) {
        gen.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptItem(this, source, item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}

// ============================================================
// HeaterGenerator
// ============================================================
class GroupHeaterGeneratorBuild extends HeaterGenerator.HeaterGeneratorBuild {

    public GroupHeaterGeneratorBuild(HeaterGenerator gen) {
        gen.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptItem(this, source, item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}

// ============================================================
// ImpactReactor
// ============================================================
class GroupImpactReactorBuild extends ImpactReactor.ImpactReactorBuild {

    public GroupImpactReactorBuild(ImpactReactor reactor) {
        reactor.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptItem(this, source, item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}

// ============================================================
// VariableReactor
// ============================================================
class GroupVariableReactorBuild extends VariableReactor.VariableReactorBuild {

    public GroupVariableReactorBuild(VariableReactor reactor) {
        reactor.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptItem(this, source, item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}

// ============================================================
// NuclearReactor
// ============================================================
class GroupNuclearReactorBuild extends NuclearReactor.NuclearReactorBuild {

    public float localHeatLastFrame;

    public GroupNuclearReactorBuild(NuclearReactor reactor) {
        reactor.super();
    }

    // reflection helpers: MindustryX may rename/remove fields
    private static float f(Object obj, Class<?> cls, String name, float def) {
        try {
            Field field = cls.getField(name);
            field.setAccessible(true);
            return field.getFloat(obj);
        } catch (Throwable t) {
            return def;
        }
    }

    private static Object o(Object obj, Class<?> cls, String name) {
        try {
            Field field = cls.getField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void updateTile(){
        GroupSupport.redirectModules(this);

        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) {
            super.updateTile();
            return;
        }

        NuclearReactor nr = (NuclearReactor) block;

        Object fuelItemObj = o(nr, NuclearReactor.class, "fuelItem");
        Item fuelItem = fuelItemObj instanceof Item it ? it : mindustry.content.Items.thorium;

        float heating = f(nr, NuclearReactor.class, "heating", 0.01f);
        float heatConsumeRate = f(nr, NuclearReactor.class, "heatConsumeRate", 10f);
        float ambientCooldownTime = f(nr, NuclearReactor.class, "ambientCooldownTime", 60f * 20f);
        float coolantPower = f(nr, NuclearReactor.class, "coolantPower", 0.5f);
        float smokeThreshold = f(nr, NuclearReactor.class, "smokeThreshold", 0.3f);
        float heatOutput = f(nr, NuclearReactor.class, "heatOutput", 12f);
        float heatWarmupRate = f(nr, NuclearReactor.class, "heatWarmupRate", 1f);
        float itemDuration = f(nr, NuclearReactor.class, "itemDuration", 120f);
        int timerFuel = (int) f(nr, NuclearReactor.class, "timerFuel", 0f);

        // use group-wide capacity as denominator to avoid fullness > 1
        int cap = block.itemCapacity * g.members.size;
        int fuel = items.get(fuelItem);
        float fullness = Mathf.clamp((float) fuel / cap);
        productionEfficiency = fullness;

        if (fuel > 0 && enabled) {
            localHeatLastFrame = fullness * heating * Math.min(delta(), 4f);
            heat += localHeatLastFrame;

            if (timer(timerFuel, itemDuration
                    / (timeScale + (heat > localHeatLastFrame ? 1f * heat * heatConsumeRate : 0f)))) {
                consume();
            }
        } else {
            productionEfficiency = 0f;
            heat = Math.max(0f, heat - Time.delta / ambientCooldownTime);
        }

        if (heat > 0) {
            float maxUsed = Math.min(liquids.currentAmount(), heat / coolantPower);
            heat -= maxUsed * coolantPower;
            liquids.remove(liquids.current(), maxUsed);
        }

        if (heat > smokeThreshold) {
            float smoke = 1.0f + (heat - smokeThreshold) / (1f - smokeThreshold);
            if (Mathf.chance(smoke / 20.0 * delta())) {
                Fx.reactorsmoke.at(
                    x + Mathf.range(block.size * Vars.tilesize / 2f),
                    y + Mathf.range(block.size * Vars.tilesize / 2f));
            }
        }

        heat = Mathf.clamp(heat);
        heatProgress = heatOutput > 0f
            ? Mathf.approachDelta(heatProgress,
                heat * heatOutput * ((enabled && productionEfficiency > 0) ? 1f : 0f),
                heatWarmupRate * delta())
            : 0f;

        if (heat >= 0.999f) {
            Events.fire(Trigger.thoriumReactorOverheat);
            kill();
        }
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptItem(this, source, item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}