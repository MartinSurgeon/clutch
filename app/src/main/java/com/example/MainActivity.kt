package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.local.ClutchDatabase
import com.example.data.repository.RoomClutchRepository
import com.example.ui.ClutchApp
import com.example.ui.ClutchViewModel
import com.example.ui.ClutchViewModelFactory
import com.example.ui.theme.ClutchTheme

class MainActivity : ComponentActivity() {

    private val database: ClutchDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ClutchDatabase::class.java,
            "clutch_ledger.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private val repository: RoomClutchRepository by lazy {
        RoomClutchRepository(database.clutchDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClutchTheme {
                val viewModel: ClutchViewModel = viewModel(
                    factory = ClutchViewModelFactory(repository)
                )
                ClutchApp(viewModel = viewModel)
            }
        }
    }
}
