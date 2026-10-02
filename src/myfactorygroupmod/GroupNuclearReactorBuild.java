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
import mindustry.world.blocks.power.NuclearReactor;

public class GroupNuclearReactorBuild extends NuclearReactor.NuclearReactorBuild implements GroupFactoryBuild {

    private FactoryGroup groupRef;

    @Override
    public FactoryGroup fgmGroup() {
        return groupRef;
    }

    @Override
    public void fgmGroup(FactoryGroup group) {
        groupRef = group;
    }

    public float localHeatLastFrame;

    public GroupNuclearReactorBuild(NuclearReactor reactor) {
        reactor.super();
    }

    // ===== 反射字段缓存 =====
    // 这些字段是方块级的，却被原实现放在 updateTile() 里，等于每个反应堆每 tick
    // 调用 10 次 Class.getField()（带同步 + 字段数组拷贝）。这里只解析一次。

    private static Field find(String name) {
        try {
            Field field = NuclearReactor.class.getField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            return null;
        }
    }

    private static final Field F_fuelItem = find("fuelItem");
    private static final Field F_heating = find("heating");
    private static final Field F_heatConsumeRate = find("heatConsumeRate");
    private static final Field F_ambientCooldownTime = find("ambientCooldownTime");
    private static final Field F_coolantPower = find("coolantPower");
    private static final Field F_smokeThreshold = find("smokeThreshold");
    private static final Field F_heatOutput = find("heatOutput");
    private static final Field F_heatWarmupRate = find("heatWarmupRate");
    private static final Field F_itemDuration = find("itemDuration");
    private static final Field F_timerFuel = find("timerFuel");

    /** 反射读字段，读不到（MindustryX 改名/删除）就用 default 值 */
    private static float f(Field field, Object obj, float def) {
        if (field == null) return def;
        try {
            return field.getFloat(obj);
        } catch (Throwable t) {
            return def;
        }
    }

    private static Object o(Field field, Object obj) {
        if (field == null) return null;
        try {
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

        Object fuelItemObj = o(F_fuelItem, nr);
        Item fuelItem = fuelItemObj instanceof Item it ? it : mindustry.content.Items.thorium;

        float heating = f(F_heating, nr, 0.01f);
        float heatConsumeRate = f(F_heatConsumeRate, nr, 10f);
        float ambientCooldownTime = f(F_ambientCooldownTime, nr, 60f * 20f);
        float coolantPower = f(F_coolantPower, nr, 0.5f);
        float smokeThreshold = f(F_smokeThreshold, nr, 0.3f);
        float heatOutput = f(F_heatOutput, nr, 12f);
        float heatWarmupRate = f(F_heatWarmupRate, nr, 1f);

        float itemDuration = f(F_itemDuration, nr, 120f);
        int timerFuel = (int) f(F_timerFuel, nr, 0f);

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
