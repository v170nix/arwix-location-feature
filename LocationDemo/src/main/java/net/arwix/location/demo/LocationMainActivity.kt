package net.arwix.location.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LocationMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_main)
        if (savedInstanceState == null) supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                LocationListFragment::class.java, null
            )
            .commit()


    }
}
