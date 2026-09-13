package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
    }

    // ===== 电力共享：群内成员互相视为连接 =====

    @Override
public arc.struct.Seq<Building> getPowerConnections(arc.struct.Seq<Building> out) {
    super.getPowerConnections(out);
    if (power == null) return out;

    FactoryGroup g = GroupManager.getGroup(this);
    if (g != null) {
        arc.struct.Seq<Building> snapshot = g.members.toSeq();
        for (Building b : snapshot) {
            if (b != this && b.power != null && b.isValid() && !out.contains(b)) {
                out.add(b);
            }
        }
    }
    return out;
}

    // ===== 每帧强制 items/liquids 指向共享池 =====

    @Override
    public void updateTile() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            items = g.sharedItems;
            if (block.hasLiquids) liquids = g.sharedLiquids;
        }

        super.updateTile();

        if (g != null && items.total() > 0) {
            for (Item item : g.getSharedOutputs()) {
                if (items.get(item) <= 0) continue;
                for (int i = 0; i < 8; i++) {
                    if (!dump(item)) break;
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            items = g.sharedItems;
            return items.get(item) < getMaximumAccepted(item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) return super.getMaximumAccepted(item);
        return block.itemCapacity * g.members.size;
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            if (block.hasLiquids) liquids = g.sharedLiquids;
            return block.hasLiquids
                && liquids.get(liquid) < block.liquidCapacity * g.members.size;
        }
        return super.acceptLiquid(source, liquid);
    }

    /** 跳过同群建筑的 dump */
    @Override
    public boolean dump(Item item) {
        if (!block.hasItems || items == null || items.total() == 0 || proximity.size == 0) return false;
        if (item != null && !items.has(item)) return false;

        FactoryGroup myG = GroupManager.getGroup(this);
        int dump = this.cdump;

        var allItems = mindustry.Vars.content.items();
        int itemSize = allItems.size;
        Object[] itemArray = allItems.items;

        if (item == null) {
            for (int i = 0; i < proximity.size; i++) {
                Building other = proximity.get((i + dump) % proximity.size);
                FactoryGroup otherG = GroupManager.getGroup(other);
                if (myG != null && otherG == myG) {
                    incrementDump(proximity.size);
                    continue;
                }

                for (int ii = 0; ii < itemSize; ii++) {
                    if (!items.has(ii)) continue;
                    Item it = (Item) itemArray[ii];
                    if (other.acceptItem(this, it) && canDump(other, it)) {
                        other.handleItem(this, it);
                        items.remove(it, 1);
                        incrementDump(proximity.size);
                        return true;
                    }
                }
                incrementDump(proximity.size);
            }
        } else {
            for (int i = 0; i < proximity.size; i++) {
                Building other = proximity.get((i + dump) % proximity.size);
                FactoryGroup otherG = GroupManager.getGroup(other);
                if (myG != null && otherG == myG) {
                    incrementDump(proximity.size);
                    continue;
                }

                if (other.acceptItem(this, item) && canDump(other, item)) {
                    other.handleItem(this, item);
                    items.remove(item, 1);
                    incrementDump(proximity.size);
                    return true;
                }
                incrementDump(proximity.size);
            }
        }
        return false;
    }

    @Override
    public void display(Table table) {
        super.display(table);

        FactoryGroup group = GroupManager.getGroup(this);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
        table.add("[lightgray]成员: []" + group.members.size).left().row();
    }
}