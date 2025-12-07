package com.example.budgetapp;

import android.os.Bundle;
import android.widget.LinearLayout; // 导入 LinearLayout
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets; // 导入 Insets
import androidx.core.view.ViewCompat; // 导入 ViewCompat
import androidx.core.view.WindowInsetsCompat; // 导入 WindowInsetsCompat
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 找到根布局 (也就是刚才设为黄色的那个 LinearLayout)
        LinearLayout rootLayout = findViewById(R.id.root_layout);

        // 2. 监听系统窗口属性，获取状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 3. 设置 Padding
            // 左、右、底保持 0，只把【顶部】往下挤一个状态栏的高度
            // 因为 rootLayout 背景是黄色，所以这个“挤出来”的顶部区域会显示黄色
            v.setPadding(0, insets.top, 0, 0);

            return windowInsets;
        });
        // --- 新增代码结束 ---

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            bottomNav.setOnItemSelectedListener(item -> {
                if (item.getItemId() != navController.getCurrentDestination().getId()) {
                    navController.navigate(item.getItemId());
                    return true;
                }
                return false;
            });
        }
    }
}