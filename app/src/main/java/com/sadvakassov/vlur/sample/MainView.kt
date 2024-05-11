package com.sadvakassov.vlur.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sadvakassov.vlur.rememberVulkanState
import com.sadvakassov.vlur.sample.theme.VlurTheme
import com.sadvakassov.vlur.vlur

@Composable
fun MainView() {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
    ) { innerPadding ->
        val vulkanState = rememberVulkanState()
        var blurIntensity by remember { mutableFloatStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sample_1),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .weight(1f)
                    .fillMaxWidth()
                    .vlur(
                        vulkanState = vulkanState,
                        blurRadius = rescale(blurIntensity, 1.0, 25.0).toFloat(),
                    )
                    .background(Color.Blue)
                ,
                contentDescription = null
            )

            Image(
                painter = painterResource(id = R.drawable.sample_2),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .weight(1f)
                    .width(250.dp)
                    .vlur(
                        vulkanState = vulkanState,
                        blurRadius = rescale(blurIntensity, 1.0, 25.0).toFloat(),
                    )
                ,
                contentDescription = null
            )

            Slider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                value = blurIntensity,
                valueRange = 0f..100f,
                onValueChange = {
                    blurIntensity = it
                }
            )
        }
    }
}

private fun rescale(progress: Float, min: Double, max: Double): Double {
    return (max - min) * (progress / 100.0) + min
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VlurTheme {
        MainView()
    }
}
