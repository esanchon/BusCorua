/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

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
import com.esanchon.buscorua.presentation.BusApplication
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
    var location: Location = Location("")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = MainPresenter(viewTranslator = this)


        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(BusApplication.appContext!!)

        if (ActivityCompat.checkSelfPermission(
                BusApplication.appContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                BusApplication.appContext!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        } else {

            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                })
                .addOnSuccessListener { location: Location? ->
                    if (location == null)
                    else {
                        presenter!!.onCreate(location)
                    }
               }
        }

        setContent {
            WearApp("?", "?")
        }
    }

    override fun showTime(busTime: BusTime) {
        busTime.let {
            setContent {
                WearApp(it.time, it.timeToHome)
            }
        }
    }

    override fun showError() {
        setContent {
            WearApp("??", "??")
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
                // Only show a Vignette for scrollable screens. This code lab only has one screen,
                // which is scrollable, so we show it all the time.
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
        text = "Al Trabajo: $timeToWork'"
    )
}

@Composable
fun TimeToHome(timeToHome: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 26.sp,
        color = MaterialTheme.colors.primary,
        text = "A casa: $timeToHome'"
    )
}

@Composable
fun TimeUpdated(timeUpdated: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = "$timeUpdated"
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
            onClick = { (context as MainActivity).presenter },
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