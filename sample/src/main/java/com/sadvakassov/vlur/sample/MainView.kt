package com.sadvakassov.vlur.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadvakassov.vlur.rememberVulkanState
import com.sadvakassov.vlur.sample.theme.VlurTheme
import com.sadvakassov.vlur.vlur
import kotlin.math.roundToInt

@Composable
fun MainView(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopBar(onClose)
        }
    ) { innerPadding ->
        val vulkanState = rememberVulkanState()
        var blurIntensity by remember { mutableFloatStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sample_2),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
                    .vlur(
                        vulkanState = vulkanState,
                        blurRadius = rescale(blurIntensity, 1.0, 25.0).toFloat(),
                    ),
                contentDescription = null
            )

            Image(
                painter = painterResource(id = R.drawable.sample_1),
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
                    .vlur(
                        vulkanState = vulkanState,
                        blurRadius = rescale(blurIntensity, 1.0, 25.0).toFloat(),
                    ),
                contentDescription = null
            )

            SliderView(blurIntensity) { newValue ->
                blurIntensity = newValue
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(onClose: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.app_name),
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background),
        navigationIcon = {
            IconButton(onClick = {
                onClose.invoke()
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp),
    )
}

@Composable
private fun SliderView(blurIntensity: Float, onBlurIntensityChanged: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp, top = 8.dp)
    ) {
        Text(
            text = "Blur value: ${blurIntensity.roundToInt()}",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            modifier = Modifier,
            value = blurIntensity,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
            valueRange = 0f..100f,
            onValueChange = {
                onBlurIntensityChanged.invoke(it)
            }
        )
    }
}

private fun rescale(progress: Float, min: Double, max: Double): Double {
    return (max - min) * (progress / 100.0) + min
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    VlurTheme {
        MainView {
            /* no-op */
        }
    }
}
