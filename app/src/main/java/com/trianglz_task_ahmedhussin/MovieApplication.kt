package com.trianglz_task_ahmedhussin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] generates the application-level
 * dependency container that every other Hilt component is a child of.
 */
@HiltAndroidApp
class MovieApplication : Application()
