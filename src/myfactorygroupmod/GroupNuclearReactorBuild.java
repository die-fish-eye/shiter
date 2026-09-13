package myfactorygroupmod;

import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.power.NuclearReactor;

public class GroupNuclearReactorBuild extends NuclearReactor.NuclearReactorBuild {

    /** 本地声明一份，避免依赖父类字段（MindustryX 运行时可能不存在） */
    public float localHeatLastFrame;

    public GroupNuclearReactorBuild(NuclearReactor reactor) {
        reactor.super();
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

        int cap = block.itemCapacity * g.members.size;
        int fuel = items.get(nr.fuelItem);
        float fullness = Mathf.clamp((float) fuel / cap);
        productionEfficiency = fullness;

        if (fuel > 0 && enabled) {
            localHeatLastFrame = fullness * nr.heating * Math.min(delta(), 4f);
            heat += localHeatLastFrame;

            if (timer(nr.timerFuel, nr.itemDuration
                    / (timeScale + (heat > localHeatLastFrame ? 1f * heat * nr.heatConsumeRate : 0f)))) {
                consume();
            }
        } else {
            productionEfficiency = 0f;
            heat = Math.max(0f, heat - Time.delta / nr.ambientCooldownTime);
        }

        if (heat > 0) {
            float maxUsed = Math.min(liquids.currentAmount(), heat / nr.coolantPower);
            heat -= maxUsed * nr.coolantPower;
            liquids.remove(liquids.current(), maxUsed);
        }

        if (heat > nr.smokeThreshold) {
            float smoke = 1.0f + (heat - nr.smokeThreshold) / (1f - nr.smokeThreshold);
            if (Mathf.chance(smoke / 20.0 * delta())) {
                mindustry.content.Fx.reactorsmoke.at(
                    x + Mathf.range(block.size * Vars.tilesize / 2f),
                    y + Mathf.range(block.size * Vars.tilesize / 2f));
            }
        }

        heat = Mathf.clamp(heat);
        heatProgress = nr.heatOutput > 0f
            ? Mathf.approachDelta(heatProgress,
                heat * nr.heatOutput * ((enabled && productionEfficiency > 0) ? 1f : 0f),
                nr.heatWarmupRate * delta())
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