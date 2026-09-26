package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;

public class GroupManager {

    private static final ObjectMap<Building, FactoryGroup> buildingToGroup = new ObjectMap<>();
    private static final ObjectMap<Building, FactoryGroup> turretToGroup = new ObjectMap<>();
    private static final ObjectMap<Building, FactoryGroup> wallToGroup = new ObjectMap<>();
    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    public static FactoryGroup getGroup(Building b) {
        FactoryGroup g = buildingToGroup.get(b);
        if (g != null) return g;
        g = turretToGroup.get(b);
        if (g != null) return g;
        return wallToGroup.get(b);
    }

    public static FactoryGroup getWallGroup(Building b) {
        return wallToGroup.get(b);
    }

    public static boolean isFactory(Building b) {
        return b instanceof GroupCrafterBuild
            || b instanceof GroupSeparatorBuild
            || b instanceof GroupDrillBuild
            || b instanceof GroupConsumeGeneratorBuild
            || b instanceof GroupHeaterGeneratorBuild
            || b instanceof GroupImpactReactorBuild
            || b instanceof GroupVariableReactorBuild
            || b instanceof GroupNuclearReactorBuild
            || b instanceof GroupPumpBuild
            || b instanceof GroupSolidPumpBuild
            || b instanceof GroupFrackerBuild
            || b instanceof GroupAttributeCrafterBuild;
    }

    public static boolean isTurret(Building b) {
        return b instanceof GroupItemTurretBuild;
    }

    public static boolean isWall(Building b) {
        return b instanceof GroupWallBuild;
    }

    public static boolean isGroupable(Building b) {
        return isFactory(b) || isTurret(b) || isWall(b);
    }

    private static void setGroup(Building b, FactoryGroup g) {
        if (isWall(b)) wallToGroup.put(b, g);
        else if (isTurret(b)) turretToGroup.put(b, g);
        else buildingToGroup.put(b, g);
    }

    private static void removeGroup(Building b) {
        wallToGroup.remove(b);
        turretToGroup.remove(b);
        buildingToGroup.remove(b);
    }

    private static void applyShared(Building b, FactoryGroup group) {
        if (isWall(b)) return; // walls share hp, not items/liquids
        if (b.items != group.sharedItems) b.items = group.sharedItems;
        if (b.block.hasLiquids && b.liquids != group.sharedLiquids) {
            b.liquids = group.sharedLiquids;
        }
    }

    private static void refreshPower(FactoryGroup group) {
        arc.struct.Seq<Building> snapshot = group.members.toSeq();
        for (Building b : snapshot) {
            if (b != null && b.power != null && b.isValid()) {
                b.updatePowerGraph();
            }
        }
    }

    public static boolean cleanup() {
        boolean changed = false;
        changed |= cleanupMap(buildingToGroup);
        changed |= cleanupMap(turretToGroup);
        changed |= cleanupMap(wallToGroup);
        return changed;
    }

    private static boolean cleanupMap(ObjectMap<Building, FactoryGroup> map) {
        boolean changed = false;
        ObjectMap<Building, FactoryGroup> copy = new ObjectMap<>(map);
        for (ObjectMap.Entry<Building, FactoryGroup> e : copy) {
            Building b = e.key;
            if (b == null || !isAlive(b)) {
                onBuildingRemoved(b);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isAlive(Building b) {
        if (b == null) return false;
        if (b.tile == null) return false;
        return b.tile.build == b;
    }

    /** same-kind check for group expansion */
    private static boolean sameKind(Building a, Building b) {
        if (isWall(a)) return isWall(b);
        if (isTurret(a)) return isTurret(b);
        return isFactory(b);
    }

    private static void enqueueNeighbors(Building b, Queue<Building> queue, ObjectSet<Building> visited) {
        int size = b.block.size;
        int soff = b.block.sizeOffset;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int tx = b.tile.x + soff + dx;
                int ty = b.tile.y + soff + dy;
                for (int[] d : DIRS) {
                    int nx = tx + d[0];
                    int ny = ty + d[1];
                    if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;
                    Tile t = Vars.world.tile(nx, ny);
                    if (t == null || t.build == null) continue;
                    if (t.build == b) continue;
                    if (!sameKind(b, t.build)) continue;
                    if (visited.contains(t.build)) continue;
                    visited.add(t.build);
                    queue.addLast(t.build);
                }
            }
        }
    }

    public static boolean hasForeignNeighbor(Building b) {
        if (!isGroupable(b)) return false;
        FactoryGroup myGroup = getGroup(b);
        if (myGroup == null) return true;
        int size = b.block.size;
        int soff = b.block.sizeOffset;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int tx = b.tile.x + soff + dx;
                int ty = b.tile.y + soff + dy;
                for (int[] d : DIRS) {
                    int nx = tx + d[0];
                    int ny = ty + d[1];
                    if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;
                    Tile t = Vars.world.tile(nx, ny);
                    if (t == null || t.build == null) continue;
                    if (t.build == b) continue;
                    if (!sameKind(b, t.build)) continue;
                    FactoryGroup otherGroup = getGroup(t.build);
                    if (otherGroup != myGroup) return true;
                }
            }
        }
        return false;
    }

    public static void onBuildingPlaced(Building building) {
        if (!isGroupable(building)) return;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();
        ObjectSet<FactoryGroup> foundGroups = new ObjectSet<>();

        queue.addLast(building);
        visited.add(building);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            FactoryGroup existing = getGroup(current);
            if (existing != null) foundGroups.add(existing);
            enqueueNeighbors(current, queue, visited);
        }

        FactoryGroup targetGroup;
        if (foundGroups.size == 0) {
            targetGroup = new FactoryGroup();
        } else {
            targetGroup = foundGroups.first();
            for (FactoryGroup other : foundGroups) {
                if (other == targetGroup) continue;
                arc.struct.Seq<Building> snapshot = other.members.toSeq();
                for (Building b : snapshot) {
                    setGroup(b, targetGroup);
                    targetGroup.members.add(b);
                }
                targetGroup.absorbItems(other);
                targetGroup.absorbLiquids(other);
            }
        }

        for (Building b : visited) {
            targetGroup.add(b);
            setGroup(b, targetGroup);
            applyShared(b, targetGroup);
        }

        // wall group: sync new member's hp with the existing fraction
        if (isWall(building)) {
            float totalMax = 0f;
            for (Building b : targetGroup.members) totalMax += b.maxHealth;
            float totalHp = targetGroup.wallHealthFraction * totalMax;
            // new wall joins at full hp
            totalHp += building.maxHealth;
            totalMax += building.maxHealth;
            if (totalMax > 0f) {
                targetGroup.wallHealthFraction = Math.min(1f, totalHp / totalMax);
            }
            for (Building b : targetGroup.members) {
                b.health = b.maxHealth * targetGroup.wallHealthFraction;
            }
        }

        refreshPower(targetGroup);
    }

    public static void onBuildingRemoved(Building building) {
        if (building == null) return;
        FactoryGroup group = getGroup(building);
        if (group == null) return;

        group.remove(building);
        removeGroup(building);

        if (group.isEmpty()) return;

        ObjectSet<Building> allMembers = new ObjectSet<>();
        allMembers.addAll(group.members);

        for (Building b : group.members) {
            removeGroup(b);
        }
        group.members.clear();

        ObjectSet<FactoryGroup> newGroups = new ObjectSet<>();
        for (Building b : allMembers) {
            if (getGroup(b) != null) continue;
            newGroups.add(bfsAssign(b, allMembers, group.wallHealthFraction));
        }

        for (FactoryGroup g : newGroups) {
            refreshPower(g);
        }
    }

    private static FactoryGroup bfsAssign(Building start, ObjectSet<Building> candidates, float inheritedWallFraction) {
        FactoryGroup newGroup = new FactoryGroup();
        newGroup.wallHealthFraction = inheritedWallFraction;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();

        queue.addLast(start);
        visited.add(start);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            newGroup.add(current);
            setGroup(current, newGroup);
            applyShared(current, newGroup);

            int size = current.block.size;
            int soff = current.block.sizeOffset;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    int tx = current.tile.x + soff + dx;
                    int ty = current.tile.y + soff + dy;
                    for (int[] d : DIRS) {
                        int nx = tx + d[0];
                        int ny = ty + d[1];
                        if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;
                        Tile t = Vars.world.tile(nx, ny);
                        if (t == null || t.build == null) continue;
                        if (t.build == current) continue;
                        if (!sameKind(current, t.build)) continue;
                        if (visited.contains(t.build)) continue;
                        if (!candidates.contains(t.build)) continue;
                        visited.add(t.build);
                        queue.addLast(t.build);
                    }
                }
            }
        }

        // re-sync wall hp to the (possibly new) fraction
        if (isWall(start)) {
            for (Building b : newGroup.members) {
                b.health = b.maxHealth * newGroup.wallHealthFraction;
            }
        }
        return newGroup;
    }
}