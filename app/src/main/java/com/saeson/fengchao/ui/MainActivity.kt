package com.saeson.fengchao.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.saeson.fengchao.App
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Config
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Config.lockEnabled && !App.unlocked) {
            startActivity(Intent(this, LockActivity::class.java))
            finish()
            return
        }
        if (!Config.hasCredentials()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener(this)
        if (savedInstanceState == null) {
            nav.selectedItemId = R.id.nav_records
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val f: Fragment = when (item.itemId) {
            R.id.nav_dashboard -> DashboardFragment()
            R.id.nav_panels -> PanelFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> RecordsFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, f)
            .commit()
        return true
    }
}
