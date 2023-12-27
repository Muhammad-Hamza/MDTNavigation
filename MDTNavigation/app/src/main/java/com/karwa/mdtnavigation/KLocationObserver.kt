package com.karwa.mdtnavigation

import android.location.Location


interface KLocationObserver
{
    fun onNewLocation(location: Location?)
}