package net.arwix.location.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LocationMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_main)

        LocationListFragment()

        supportFragmentManager.apply {
            val listFragment = this.findFragmentByTag("LocationListFragment")
            if (listFragment == null) {
                this.beginTransaction()
                    .add(R.id.fragment_container, LocationListFragment(), "LocationListFragment")
                    .commit()
            } else {
                if (listFragment.isDetached) this.beginTransaction().replace(
                    R.id.fragment_container,
                    listFragment
                ).commit()
            }
        }

    }
}
