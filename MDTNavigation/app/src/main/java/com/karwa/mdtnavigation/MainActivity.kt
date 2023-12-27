package com.karwa.mdtnavigation

import android.R.attr.data
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            val route: String? = intent.data?.getQueryParameter("ROUTE_INTENT")
            Toast.makeText(applicationContext, route,Toast.LENGTH_SHORT).show()
        }
    }
}
