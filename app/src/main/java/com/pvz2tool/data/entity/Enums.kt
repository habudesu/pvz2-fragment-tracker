package com.pvz2tool.data.entity

/** 五阶状态：决定能否升到 5 阶，以及卡片何时算「已完成」。 */
enum class FifthTierStatus {
    HAS_FIFTH,      // 已开放五阶
    PENDING_FIFTH,  // 暂 4 阶封顶，将来会开放五阶
    NEVER_FIFTH     // 无进阶 / 立即生效型，封顶 1 或 3
}

/**
 * 状态的单向顺序：NEVER(0) → PENDING(1) → HAS(2)。
 * 只允许往数值更大的方向改，防止「降最大阶数」。
 */
val FifthTierStatus.orderRank: Int
    get() = when (this) {
        FifthTierStatus.NEVER_FIFTH -> 0
        FifthTierStatus.PENDING_FIFTH -> 1
        FifthTierStatus.HAS_FIFTH -> 2
    }

/** 品质颜色：白→绿→蓝→紫→橙（顺序即展示顺序）。 */
enum class PlantColor(val displayName: String) {
    WHITE("白"),
    GREEN("绿"),
    BLUE("蓝"),
    PURPLE("紫"),
    ORANGE("橙")
}
