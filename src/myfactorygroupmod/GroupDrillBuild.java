package myfactorygroupmod;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.Drill;

public class GroupDrillBuild extends Drill.DrillBuild {

    public GroupDrillBuild(Drill drill) {
        drill.super();
    }

    @Override
    public void updateTile(){
        GroupSupport.redirectModules(this);

        Drill drill = (Drill) block;
        FactoryGroup g = GroupManager.getGroup(this);
        int cap = (g != null && g.members.size > 0)
            ? block.itemCapacity * g.members.size
            : block.itemCapacity;

        if (dominantItem == null) {
            GroupSupport.extraDump(this);
            return;
        }

        timeDrilled += warmup * delta();
        float delay = drill.getDrillTime(dominantItem);

        // 关键：只检查自己的产物，不检查整个共享池的总物品数
        if (items.get(dominantItem) < cap && dominantItems > 0 && efficiency > 0) {
            float speed = Mathf.lerp(1f, drill.liquidBoostIntensity, optionalEfficiency) * efficiency;
            lastDrillSpeed = (speed * dominantItems * warmup) / delay;
            warmup = Mathf.approachDelta(warmup, speed, drill.warmupSpeed);
            progress += delta() * dominantItems * speed * warmup;

            if (Mathf.chanceDelta(drill.updateEffectChance * warmup)) {
                drill.updateEffect.at(x + Mathf.range(block.size * 2f), y + Mathf.range(block.size * 2f));
            }
        } else {
            lastDrillSpeed = 0f;
            warmup = Mathf.approachDelta(warmup, 0f, drill.warmupSpeed);
            GroupSupport.extraDump(this);
            return;
        }

        if (dominantItems > 0 && progress >= delay && items.get(dominantItem) < cap) {
            int amount = (int) (progress / delay);
            for (int i = 0; i < amount; i++) {
                offload(dominantItem);
            }
            progress %= delay;

            if (wasVisible && Mathf.chanceDelta(drill.drillEffectChance * warmup)) {
                drill.drillEffect.at(
                    x + Mathf.range(drill.drillEffectRnd),
                    y + Mathf.range(drill.drillEffectRnd),
                    dominantItem.color);
            }
        }

        GroupSupport.extraDump(this);
    }

    @Override
    public boolean shouldConsume(){
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) return super.shouldConsume();
        if (dominantItem == null) return false;
        int cap = block.itemCapacity * g.members.size;
        return items.get(dominantItem) < cap && enabled;
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptLiquid(this, source, liquid);
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    public boolean dump(Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.dumpShared(this, item);
        }
        return super.dump(item);
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