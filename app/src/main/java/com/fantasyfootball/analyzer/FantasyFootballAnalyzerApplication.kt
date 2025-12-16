package com.fantasyfootball.analyzer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Fantasy Football Analyzer.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class FantasyFootballAnalyzerApplication : Application()