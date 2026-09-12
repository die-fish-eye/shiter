package myfactorygroupmod;

import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import mindustry.gen.Building;
import mindustry.ui.dialogs.BaseDialog;

public class GroupInfoDialog extends BaseDialog {

    public GroupInfoDialog() {
        super("工厂群信息");
        addCloseButton();
    }

    /** 显示指定建筑所属工厂群的信息 */
    public void showFor(Building building) {
        FactoryGroup group = GroupManager.getGroup(building);
        if (group == null) {
            cont.clear();
            cont.add("该建筑不属于任何工厂群").row();
            show();
            return;
        }

        cont.clear();

        // 显示群组成
        cont.add(group.describeComposition()).left().row();

        // 显示共享物品详情
        cont.add("共享库存：").left().row();

        Table itemTable = new Table();
        for (mindustry.type.Item item : mindustry.Vars.content.items()) {
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

    /** 注册到游戏中，拦截建筑点击 */
    public static void register() {
        // 通过 Events 监听建筑选择事件
        arc.Events.on(mindustry.game.EventType.BuildSelectEvent.class, e -> {
            if (e.building != null && e.building.block.hasItems) {
                FactoryGroup group = GroupManager.getGroup(e.building);
                if (group != null && group.members.size > 1) {
                    // 显示自定义对话框，替代原版库存显示
                    Dialog dialog = new GroupInfoDialog();
                    ((GroupInfoDialog) dialog).showFor(e.building);
                }
            }
        });
    }
}