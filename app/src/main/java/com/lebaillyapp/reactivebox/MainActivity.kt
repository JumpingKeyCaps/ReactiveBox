package com.lebaillyapp.reactivebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lebaillyapp.reactivebox.ui.composition.ReactiveBoxStatic
import com.lebaillyapp.reactivebox.ui.composition.SensorCrosshairDebug
import com.lebaillyapp.reactivebox.ui.composition.SensorCrosshairDebugOverkill
import com.lebaillyapp.reactivebox.ui.theme.ReactiveBoxTheme
import com.lebaillyapp.reactivebox.ui.viewModel.SensorViewModel

class MainActivity : ComponentActivity() {


    private val sensorViewModel: SensorViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReactiveBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box( modifier = Modifier.fillMaxSize().padding(innerPadding) ){

                        //debug sensor
                   //     SensorCrosshairDebug(viewModel = sensorViewModel)
                    //    SensorCrosshairDebugOverkill(viewModel = sensorViewModel)


                        ReactiveBoxStatic()

                    }
                }
            }
        }
    }
}
