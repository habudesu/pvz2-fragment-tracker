package com.pvz2tool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pvz2tool.ui.AppRoot

/**
 * 单 Activity 入口。
 * 后续所有界面（主页 / 设置）都由 Compose 承载，不在 Activity 里写业务逻辑。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 边到边显示，配合 Compose 的 insets
        setContent {
            AppRoot()
        }
    }
}
