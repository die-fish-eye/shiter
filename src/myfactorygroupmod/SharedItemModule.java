package myfactorygroupmod;

import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.modules.ItemModule;

/**
 * 一个"代理" ItemModule：读写全部转发到所属工厂群的 sharedItems。
 * 如果建筑不属于任何群，则回退到本地父类实现。
 */
public class SharedItemModule extends ItemModule {

    public final Building owner;

    public SharedItemModule(Building owner) {
        this.owner = owner;
    }

    private ItemModule target() {
        FactoryGroup g = GroupManager.getGroup(owner);
        return g == null ? null : g.sharedItems;
    }

    @Override
    public int get(Item item) {
        ItemModule t = target();
        return t == null ? super.get(item) : t.get(item);
    }

    @Override
    public int get(int id) {
        ItemModule t = target();
        return t == null ? super.get(id) : t.get(id);
    }

    @Override
    public void set(Item item, int amount) {
        ItemModule t = target();
        if (t == null) super.set(item, amount);
        else t.set(item, amount);
    }

    @Override
    public void add(Item item, int amount) {
        ItemModule t = target();
        if (t == null) super.add(item, amount);
        else t.add(item, amount);
    }

    @Override
    public void remove(Item item, int amount) {
        ItemModule t = target();
        if (t == null) super.remove(item, amount);
        else t.remove(item, amount);
    }

    @Override
    public boolean has(Item item, int amount) {
        ItemModule t = target();
        return t == null ? super.has(item, amount) : t.has(item, amount);
    }

    @Override
    public int total() {
        ItemModule t = target();
        return t == null ? super.total() : t.total();
    }

    @Override
    public boolean any() {
        ItemModule t = target();
        return t == null ? super.any() : t.any();
    }

    @Override
    public boolean isEmpty() {
        ItemModule t = target();
        return t == null ? super.isEmpty() : t.isEmpty();
    }

    @Override
    public void clear() {
        ItemModule t = target();
        if (t == null) super.clear();
        else t.clear();
    }
}