package com.pvz2tool.domain

/** 主页二级分组。 */
enum class DisplaySection {
    IN_PROGRESS,   // 进行中：还没到顶阶
    PENDING_MAX,   // 已满待五阶：顶阶 4，未来还会出 5 阶，永远显示
    COMPLETED      // 已完成：到顶阶且不再升，整体调暗
}
