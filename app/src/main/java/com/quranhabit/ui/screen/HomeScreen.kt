package com.quranhabit.ui.screen

import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Surface {
        Button(onClick = { navController.navigate("details") }) {
            Text("Go to Details")
        }
    }
}