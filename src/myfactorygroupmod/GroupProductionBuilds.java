package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.AttributeCrafter;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Separator;

/**
 * Production-type Group*Build classes.
 *  - GroupCrafterBuild
 *  - GroupSeparatorBuild
 *  - GroupAttributeCrafterBuild
 *  - GroupDrillBuild
 */
public final class GroupProductionBuilds {
    private GroupProductionBuilds() {}
}

// ============================================================
// GenericCrafter
// ============================================================
class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
        GroupSupport.extraDump(this);
    }

    @Override
    public boolean shouldConsume(){
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.shouldConsume();

        int cap = block.itemCapacity * g.members.size;
        if (block instanceof GenericCrafter gc) {
            if (gc.outputItem != null && items.get(gc.outputItem.item) >= cap) return false;
            if (gc.outputItems != null) {
                for (ItemStack out : gc.outputItems) {
                    if (items.get(out.item) >= cap) return false;
                }
            }
        }
        return enabled;
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

// ============================================================
// Separator
// ============================================================
class GroupSeparatorBuild extends Separator.SeparatorBuild {

    public GroupSeparatorBuild(Separator separator) {
        separator.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
        GroupSupport.extraDump(this);
    }

    @Override
    public boolean shouldConsume(){
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) return super.shouldConsume();

        int cap = block.itemCapacity * g.members.size;
        int productTotal = 0;
        if (block instanceof Separator sep && sep.results != null) {
            for (ItemStack r : sep.results) {
                productTotal += items.get(r.item);
            }
        }
        return productTotal < cap && enabled;
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

// ============================================================
// AttributeCrafter (cultivator / silicon-crucible / vent-condenser)
// ============================================================
class GroupAttributeCrafterBuild extends AttributeCrafter.AttributeCrafterBuild {

    public GroupAttributeCrafterBuild(AttributeCrafter crafter) {
        crafter.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
        GroupSupport.extraDump(this);
    }

    @Override
    public boolean shouldConsume(){
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.shouldConsume();

        int cap = block.itemCapacity * g.members.size;
        if (block instanceof AttributeCrafter ac) {
            if (ac.outputItem != null && items.get(ac.outputItem.item) >= cap) return false;
            if (ac.outputItems != null) {
                for (ItemStack out : ac.outputItems) {
                    if (items.get(out.item) >= cap) return false;
                }
            }
        }
        return enabled;
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

// ============================================================
// Drill
// ============================================================
class GroupDrillBuild extends Drill.DrillBuild {

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

        // only care about our own output, not total shared pool
        if (items.get(dominantItem) < cap && dominantItems > 0 && efficiency > 0) {
            float speed = arc.math.Mathf.lerp(1f, drill.liquidBoostIntensity, optionalEfficiency) * efficiency;
            lastDrillSpeed = (speed * dominantItems * warmup) / delay;
            warmup = arc.math.Mathf.approachDelta(warmup, speed, drill.warmupSpeed);
            progress += delta() * dominantItems * speed * warmup;

            if (arc.math.Mathf.chanceDelta(drill.updateEffectChance * warmup)) {
                drill.updateEffect.at(x + arc.math.Mathf.range(block.size * 2f),
                                     y + arc.math.Mathf.range(block.size * 2f));
            }
        } else {
            lastDrillSpeed = 0f;
            warmup = arc.math.Mathf.approachDelta(warmup, 0f, drill.warmupSpeed);
            GroupSupport.extraDump(this);
            return;
        }

        if (dominantItems > 0 && progress >= delay && items.get(dominantItem) < cap) {
            int amount = (int) (progress / delay);
            for (int i = 0; i < amount; i++) {
                offload(dominantItem);
            }
            progress %= delay;

            if (wasVisible && arc.math.Mathf.chanceDelta(drill.drillEffectChance * warmup)) {
                drill.drillEffect.at(x + arc.math.Mathf.range(drill.drillEffectRnd),
                                     y + arc.math.Mathf.range(drill.drillEffectRnd),
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