package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.Queue;
import mindustry.gen.Building;
import mindustry.world.Tile;

public class GroupManager {

    /** 建筑 → 所属工厂群 */
    private static final ObjectMap<Building, FactoryGroup> buildingToGroup = new ObjectMap<>();

    /** 获取建筑所属的工厂群 */
    public static FactoryGroup getGroup(Building building) {
        return buildingToGroup.get(building);
    }

    /** 建筑被放置时调用 */
    public static void onBuildingPlaced(Building building) {
        // 只有拥有物品模块的方块才参与工厂群
        if (!building.block.hasItems) return;

        // 如果已经属于某个群，跳过
        if (buildingToGroup.containsKey(building)) return;

        // BFS 查找所有相连的工厂群
        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();
        ObjectSet<FactoryGroup> foundGroups = new ObjectSet<>();

        queue.addLast(building);
        visited.add(building);

        while (queue.size > 0) {
            Building current = queue.removeFirst();

            // 检查当前建筑是否已属于某个群
            FactoryGroup existing = buildingToGroup.get(current);
            if (existing != null) {
                foundGroups.add(existing);
            }

            // 遍历四个方向的相邻格子
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};

            for (int i = 0; i < 4; i++) {
                int nx = current.tile.x + dx[i];
                int ny = current.tile.y + dy[i];

                if (nx < 0 || ny < 0 || nx >= mindustry.Vars.world.width()
                        || ny >= mindustry.Vars.world.height()) continue;

                Tile neighbor = mindustry.Vars.world.tile(nx, ny);
                if (neighbor == null || neighbor.build == null) continue;
                if (!neighbor.build.block.hasItems) continue;
                if (visited.contains(neighbor.build)) continue;

                visited.add(neighbor.build);
                queue.addLast(neighbor.build);
            }
        }

        // 确定使用哪个群：如果找到多个已有群，合并它们
        FactoryGroup targetGroup;
        if (foundGroups.size == 0) {
            // 全新群
            targetGroup = new FactoryGroup();
        } else if (foundGroups.size == 1) {
            targetGroup = foundGroups.first();
        } else {
            // 合并多个群
            targetGroup = foundGroups.first();
            for (FactoryGroup other : foundGroups) {
                if (other == targetGroup) continue;
                // 把 other 的所有成员迁移到 targetGroup
                for (Building b : other.members) {
                    buildingToGroup.put(b, targetGroup);
                    targetGroup.members.add(b);
                    // 把 other 的库存合并进来
                    targetGroup.sharedItems.add(other.sharedItems);
                }
            }
        }

        // 把本次扫描到的所有工厂加入目标群
        for (Building b : visited) {
            targetGroup.add(b);
            buildingToGroup.put(b, targetGroup);
        }
    }

    /** 建筑被拆除时调用 */
    public static void onBuildingRemoved(Building building) {
        FactoryGroup group = buildingToGroup.get(building);
        if (group == null) return;

        group.remove(building);
        buildingToGroup.remove(building);

        // 如果群已空，不需要额外操作
        if (group.isEmpty()) return;

        // 检查拆除后群是否分裂成多个不连通的部分
        // 简化处理：对剩余成员做一次 BFS，如果发现不连通，重新分组
        ObjectSet<Building> allMembers = new ObjectSet<>();
        allMembers.addAll(group.members);

        // 清空原有映射，重新扫描
        for (Building b : group.members) {
            buildingToGroup.remove(b);
        }
        group.members.clear();
        group.sharedItems.clear();

        // 对每个未分配的成员做 BFS
        for (Building b : allMembers) {
            if (buildingToGroup.containsKey(b)) continue;
            // 复用放置逻辑，但这里直接调用 BFS 方法
            bfsAssign(b);
        }
    }

    /** 对一个起始建筑做 BFS，分配到一个新的工厂群 */
    private static void bfsAssign(Building start) {
        FactoryGroup newGroup = new FactoryGroup();
        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();

        queue.addLast(start);
        visited.add(start);

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            newGroup.add(current);
            buildingToGroup.put(current, newGroup);

            for (int i = 0; i < 4; i++) {
                int nx = current.tile.x + dx[i];
                int ny = current.tile.y + dy[i];

                if (nx < 0 || ny < 0 || nx >= mindustry.Vars.world.width()
                        || ny >= mindustry.Vars.world.height()) continue;

                Tile neighbor = mindustry.Vars.world.tile(nx, ny);
                if (neighbor == null || neighbor.build == null) continue;
                if (!neighbor.build.block.hasItems) continue;
                if (visited.contains(neighbor.build)) continue;
                // 只处理属于原群但尚未重新分配的
                if (!allMembersContains(neighbor.build)) continue;

                visited.add(neighbor.build);
                queue.addLast(neighbor.build);
            }
        }
    }

    /** 辅助：检查建筑是否在原群成员集合中 */
    private static boolean allMembersContains(Building b) {
        // 简化实现：检查 buildingToGroup 是否已包含它
        return !buildingToGroup.containsKey(b);
    }
}