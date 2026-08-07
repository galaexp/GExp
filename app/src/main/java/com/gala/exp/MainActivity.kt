package com.gala.exp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.gala.exp.ui.GalaAppScreen
import com.gala.exp.ui.theme.MyApplicationTheme
import com.gala.exp.vm.GalaViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: GalaViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        GalaAppScreen(viewModel = viewModel)
      }
    }
  }
}
