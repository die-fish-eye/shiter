package myfactorygroupmod;

import arc.Events;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.ui.dialogs.BaseDialog;

public class GroupInfoDialog extends BaseDialog {

    private static GroupInfoDialog instance;

    public GroupInfoDialog() {
        super("工厂群信息");
        addCloseButton();
    }

    /** 显示指定建筑所属工厂群的信息 */
    public void showFor(Building building) {
        cont.clear();

        FactoryGroup group = GroupManager.getGroup(building);
        if (group == null || group.members.size <= 1) {
            cont.add("该建筑不属于任何工厂群").row();
            show();
            return;
        }

        cont.add(group.describeComposition()).left().row();
        cont.add("共享库存：").left().row();

        Table itemTable = new Table();
        for (Item item : Vars.content.items()) {
            int amount = group.sharedItems.get(item);
            if (amount > 0) {
                itemTable.image(item.uiIcon).size(24f);
                itemTable.add(item.localizedName + ": " + amount).left().padLeft(4f);
                itemTable.row();
            }
        }
        cont.add(itemTable).left().row();

        show();
    }

    /** 注册事件监听 */
    public static void register() {
        instance = new GroupInfoDialog();

        Events.on(BuildSelectEvent.class, e -> {
            if (e == null || e.tile == null) return;
            Building building = e.tile.build;
            if (building == null) return;
            if (!building.block.hasItems) return;

            FactoryGroup group = GroupManager.getGroup(building);
            if (group != null && group.members.size > 1) {
                instance.showFor(building);
            }
        });
    }
}