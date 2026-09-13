package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.power.NuclearReactor;

public class GroupNuclearReactorBuild extends NuclearReactor.NuclearReactorBuild {

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

        // 用群总容量作为基准，避免共享池钍太多导致 fullness > 1
        int cap = block.itemCapacity * g.members.size;
        int fuel = items.get(fuelItem);
        float fullness = Mathf.clamp((float) fuel / cap);
        productionEfficiency = fullness;

        if (fuel > 0 && enabled) {
            heat += heatLastFrame = fullness * heating * Math.min(delta(), 4f);

            if (timer(timerFuel, itemDuration / (timeScale + (heat > heatLastFrame ? 1f * heat * heatConsumeRate : 0f)))) {
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
                mindustry.content.Fx.reactorsmoke.at(
                    x + Mathf.range(size * mindustry.Vars.tilesize / 2f),
                    y + Mathf.range(size * mindustry.Vars.tilesize / 2f));
            }
        }

        heat = Mathf.clamp(heat);
        heatProgress = heatOutput > 0f
            ? Mathf.approachDelta(heatProgress,
                heat * heatOutput * ((enabled && productionEfficiency > 0) ? 1f : 0f),
                heatWarmupRate * delta())
            : 0f;

        if (heat >= 0.999f) {
            mindustry.game.EventType.Trigger.thoriumReactorOverheat.fire();
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