package myfactorygroupmod;

/**
 * 标记接口：所有参与“工厂群”的建筑都实现它。
 * <p>
 * 这样 {@code isGroupable()} 只需要一次 instanceof 接口判定，
 * 而不是原来的十几次具体类 instanceof 链 —— 该方法在邻居扫描、
 * 全图兜底扫描等热路径上会被反复调用。
 */
public interface GroupBuild {
    /** 本建筑所属的工厂群；未入群时为 null（字段直读，0 次哈希）。 */
    FactoryGroup fgmGroup();

    /** 设置本建筑所属的工厂群；传 null 表示脱离群。 */
    void fgmGroup(FactoryGroup group);
}
