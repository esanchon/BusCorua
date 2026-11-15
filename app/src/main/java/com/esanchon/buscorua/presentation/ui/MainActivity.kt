package com.esanchon.buscorua.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import com.esanchon.buscorua.presentation.domain.BusTime
import com.esanchon.buscorua.presentation.presentation.MainPresenter
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener

class MainActivity : ComponentActivity(), ViewTranslator {

    var presenter: MainPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = MainPresenter(viewTranslator = this)
        loadBusTimes()
    }

    fun loadBusTimes() {
        setContent {
            WearApp("...", "...")
        }

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
            loadWithDefaultLocation()
        } else {
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                })
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        loadWithDefaultLocation()
                    } else {
                        presenter?.onCreate(location)
                    }
                }
                .addOnFailureListener {
                    loadWithDefaultLocation()
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            loadBusTimes()
        }
    }

    private fun loadWithDefaultLocation() {
        val defaultLocation = Location("").apply {
            latitude = 43.37135
            longitude = -8.396
        }
        presenter?.onCreate(defaultLocation)
    }

    override fun showTime(busTime: BusTime) {
        setContent {
            WearApp(busTime.time ?: "?", busTime.timeToHome ?: "?")
        }
    }

    override fun showError() {
        setContent {
            WearApp("Error", "Error")
        }
    }
}

@Composable
fun WearApp(timeToWork: String, timeToHome: String) {
    BusCoruñaTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                TimeText(
                    timeSource = TimeTextDefaults.timeSource(timeFormat())
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {

            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
            val iconModifier = Modifier
                .size(28.dp)
                .wrapContentSize(align = Alignment.Center)

            ScalingLazyColumn(
                modifier = contentModifier,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            )
            {
                item { TimeToWork(timeToWork = timeToWork) }
                item { TimeToHome(timeToHome = timeToHome) }
                item { TimeUpdated(timeUpdated = "") }
                item { ButtonUpdate(contentModifier, iconModifier) }

            }
        }
    }
}


@Composable
fun TimeToWork(timeToWork: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 26.sp,
        color = MaterialTheme.colors.primary,
        text = "Al Trabajo: $timeToWork"
    )
}

@Composable
fun TimeToHome(timeToHome: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 26.sp,
        color = MaterialTheme.colors.primary,
        text = "A casa: $timeToHome"
    )
}

@Composable
fun TimeUpdated(timeUpdated: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = timeUpdated
    )
}

@Composable
fun ButtonUpdate(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        // Button
        Button(
            modifier = Modifier.size(ButtonDefaults.DefaultButtonSize),
            onClick = {
                (context as? MainActivity)?.loadBusTimes()
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "triggers update action",
                modifier = iconModifier
            )
        }
    }
}

@Composable
fun ChipTime(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier
) {
    Chip(
        modifier = modifier,
        onClick = { /* ... */ },
        label = {
            Text(
                text = "5 minute Meditation",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SelfImprovement,
                contentDescription = "triggers meditation action",
                modifier = iconModifier
            )
        },
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("?", "?")
}
