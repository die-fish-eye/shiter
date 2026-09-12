package myfactorygroupmod;

import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.modules.LiquidModule;

public class SharedLiquidModule extends LiquidModule {

    public final Building owner;

    public SharedLiquidModule(Building owner) {
        this.owner = owner;
    }

    private LiquidModule target() {
        FactoryGroup g = GroupManager.getGroup(owner);
        return g == null ? null : g.sharedLiquids;
    }

    @Override
    public float currentAmount() {
        LiquidModule t = target();
        return t == null ? super.currentAmount() : t.currentAmount();
    }

    @Override
    public float get(Liquid liquid) {
        LiquidModule t = target();
        return t == null ? super.get(liquid) : t.get(liquid);
    }

    @Override
    public void set(Liquid liquid, float amount) {
        LiquidModule t = target();
        if (t == null) super.set(liquid, amount);
        else t.set(liquid, amount);
    }

    @Override
    public void add(Liquid liquid, float amount) {
        LiquidModule t = target();
        if (t == null) super.add(liquid, amount);
        else t.add(liquid, amount);
    }

    @Override
    public void remove(Liquid liquid, float amount) {
        LiquidModule t = target();
        if (t == null) super.remove(liquid, amount);
        else t.remove(liquid, amount);
    }

    @Override
    public void clear() {
        LiquidModule t = target();
        if (t == null) super.clear();
        else t.clear();
    }
}