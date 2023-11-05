package com.maja.basicruler

import android.content.res.Configuration.DENSITY_DPI_UNDEFINED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
/*import androidx.compose.ui.tooling.preview.Preview*/
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maja.basicruler.ui.theme.BasicRulerTheme
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var dpi = this.resources.configuration.densityDpi
        val tickLength = 30
        if (dpi == DENSITY_DPI_UNDEFINED) {
            dpi = 240
        }
        setContent {
            BasicRulerTheme {
                val context = LocalContext.current
                val preferencesManager = remember { PreferencesManager(context) }
                val keyCalibration = stringResource(R.string.correction_factor)
                val prevCalibration = preferencesManager.getData(keyCalibration, 1f.toString()).toFloat()
                val currCalibration = remember { mutableStateOf(prevCalibration) }
                val keyUnit = stringResource(R.string.unit)
                val prevUnit = preferencesManager.getData(keyUnit, "cm")
                val currUnit = remember { mutableStateOf(prevUnit) }

                Surface(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)) {

                    DrawTicks(dpi = dpi, tickLength = tickLength, vertical = true, factor = currCalibration, unit = currUnit)
                    DrawLabels(dpi = dpi, tickLength = tickLength, vertical = true, factor = currCalibration, unit = currUnit)
                    DrawOptions(dpi = dpi, factor = currCalibration, unit = currUnit)
                }
            }
        }
    }
}

@Composable
fun DrawTicks(dpi: Int, tickLength: Int = 30, vertical: Boolean = true, factor: MutableState<Float>, unit: MutableState<String>) {
    val colorRect = MaterialTheme.colorScheme.primary
    val colorTicks = MaterialTheme.colorScheme.background
    val tickLength1 = tickLength * 0.7f
    val tickLength5 = tickLength * 0.8f
    val unitDist = if (unit.value == "cm") 2.54f else 1f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidthMain = (tickLength / 20).dp.toPx()
        val strokeWidthIntermediate = (tickLength / 30).dp.toPx()
        val rectWidth = (tickLength*2.5f).dp.toPx()
        val mm = dpi.toFloat() * (1f / unitDist) / 10f * factor.value
        val mmNum = if (vertical) (size.height / mm).toInt() else (size.width / mm).toInt()
        val offsetX = 0f
        var rect = Size(width = rectWidth, height = size.height)

        if (!vertical) {
            rect = Size(width = size.width, height = rectWidth)
        }

        drawRect(color = colorRect, size = rect)

        for (distance in 0..mmNum) {
            val y = mm*distance
            var start = Offset(x = offsetX, y = y)
            var endX = offsetX + tickLength1.dp.toPx()
            var stroke = strokeWidthIntermediate

            if (distance % 10 == 0) {
                endX = offsetX + tickLength.dp.toPx()
                stroke = strokeWidthMain
            } else if (distance % 5 == 0) {
                endX = offsetX + tickLength5.dp.toPx()
                stroke = strokeWidthIntermediate
            }

            var end = Offset(x = endX, y = y)
            if (!vertical) {
                end = Offset(x = end.y, y = end.x)
                start = Offset(x = start.y, y = start.x)
            }
            drawLine(start = start, end = end, color = colorTicks, strokeWidth = stroke)
        }

    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DrawLabels(dpi: Int, tickLength: Int = 30, vertical: Boolean = true, factor: MutableState<Float>, unit: MutableState<String>) {
    val textMeasure = rememberTextMeasurer()
    val offsetY = tickLength + tickLength/2f
    val color = MaterialTheme.colorScheme.background
    val unitDist = if (unit.value == "cm") 2.54f else 1f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val offset = dpi.toFloat() * (1f / unitDist) * factor.value
        val counter = if (vertical) (size.height / offset).toInt() else (size.width / offset).toInt()

        for (distance in 0..counter) {
            val text = AnnotatedString(distance.toString())
            val style = TextStyle(fontSize = 16.sp, color = color)
            val textLayoutResult = textMeasure.measure(text = text, style = style)
            val textSize = textLayoutResult.size

            if (vertical) {
                rotate(degrees = 90f, pivot = Offset(x = 0f, y = 0f)) {
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = -textSize.width / 2f + distance * offset,
                            y = -textSize.height / 2f - offsetY.dp.toPx()
                        )
                    )
                }
            } else {
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = distance * offset - textSize.width / 2f,
                        y = offsetY.dp.toPx() - textSize.height / 2f
                    )
                )
            }
        }
    }
}

@Composable
fun DrawOptions(dpi: Int, factor: MutableState<Float>, unit: MutableState<String>) {
    val showSetting = remember { mutableStateOf(false) }
    val showMeasure = remember { mutableStateOf(false) }
    // draw this on the bottom layer
    if (showMeasure.value) {
        ShowMeasure(factor, unit, dpi)
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
        var color = MaterialTheme.colorScheme.primary
        if (showMeasure.value)
            color = color.copy(0.5f)
        Button(onClick = { showMeasure.value = !showMeasure.value },
            modifier = Modifier
                .padding(10.dp)
                .size(60.dp),
            shape = CircleShape,
            border = null, //BorderStroke(0.dp, Color(0XFFFFFFFF)),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Icon(Icons.Default.Create, contentDescription = "Measure", Modifier.size(30.dp), tint=MaterialTheme.colorScheme.background)
        }
        Button(onClick = { showSetting.value = true },
            modifier = Modifier
                .padding(10.dp)
                .size(60.dp),
            shape = CircleShape,
            border = null, //BorderStroke(0.dp, Color(0XFFFFFFFF)),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", Modifier.size(30.dp), tint=MaterialTheme.colorScheme.background)
        }
    }
    if (showSetting.value) {
        ShowSettingDialog(factor, unit, showSetting, dpi)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ShowMeasure(factor: MutableState<Float>, unit: MutableState<String>, dpi: Int) {
    val color = MaterialTheme.colorScheme.secondary.copy(0.85f)
    val txtColor = MaterialTheme.colorScheme.primary
    val dim = remember { mutableStateOf(Size(width = 300f, height = 300f)) }
    val center = remember { mutableStateOf(Offset(x = 300f + dim.value.width/2f, y = 300f + dim.value.height/2f)) }
    val unitDist = if (unit.value == "cm") 2.54f else 1f
    val offset = dpi.toFloat() * (1f / unitDist) * factor.value
    val textMeasure = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(key1 = Unit) {
        detectDragGestures(
            onDrag = { change, delta ->
                val currPos = change.position
                // check for close center this enables drag&drop
                val radius = sqrt((center.value.x - currPos.x).pow(2) + (center.value.y - currPos.y).pow(2))
                if (radius < min(100f, (dim.value.width + dim.value.height) / 2f)) {
                    center.value = Offset(x = center.value.x + delta.x, y = center.value.y + delta.y)
                } else if (abs(delta.x) > abs(delta.y)) { // movement is in x-direction
                    // resize rectangle on the right side
                    if (currPos.x > center.value.x) {
                        center.value = Offset(x = center.value.x + delta.x/2f, y = center.value.y)
                        dim.value = Size(width = dim.value.width + delta.x, height = dim.value.height)
                    } else { // resize rectangle on the left side
                        center.value = Offset(x = center.value.x + delta.x/2f, y = center.value.y)
                        dim.value = Size(width = dim.value.width - delta.x, height = dim.value.height)
                    }
                } else { // movement is in y direction
                    // resize rectangle on top side
                    if (currPos.y < center.value.y) {
                        center.value = Offset(x = center.value.x, y = center.value.y + delta.y/2f)
                        dim.value = Size(width = dim.value.width, height = dim.value.height - delta.y)
                    } else {
                        // resize rectangle on bottom side
                        center.value = Offset(x = center.value.x, y = center.value.y + delta.y/2f)
                        dim.value = Size(width = dim.value.width, height = dim.value.height + delta.y)
                    }
                }
            }
        )
    }) {
        // calc screen limits of rectangle
        // if negative the rect is overhanging on the right side
        val dXR = size.width - (center.value.x + dim.value.width/2f)
        if (dXR < 0) {
            center.value = Offset(x = center.value.x - abs(dXR)/2f, y = center.value.y)
            dim.value = Size(width = dim.value.width - abs(dXR), height = dim.value.height)
        }
        // if negative the rect is overhanging on the left side
        val dXL = center.value.x - dim.value.width/2f
        if (dXL < 0) {
            center.value = Offset(x = center.value.x + abs(dXL)/2f, y = center.value.y)
            dim.value = Size(width = dim.value.width - abs(dXL), height = dim.value.height)
        }
        // if negative the rect is overhanging on the top side
        val dYT = center.value.y - dim.value.height/2f
        if (dYT < 0) {
            center.value = Offset(x = center.value.x, y = center.value.y + abs(dYT)/2f)
            dim.value = Size(width = dim.value.width, height = dim.value.height - abs(dYT))
        }
        // if negative the rect is overhanging on the bottom side
        val dYB = size.height - (center.value.y + dim.value.height/2f)
        if (dYB < 0) {
            center.value = Offset(x = center.value.x, y = center.value.y - abs(dYB)/2f)
            dim.value = Size(width = dim.value.width, height = dim.value.height - abs(dYB))
        }

        // draw size of rectangle
        val w = dim.value.width/offset
        val h = dim.value.height/offset
        val style = TextStyle(fontSize = 20.sp, color = color, textAlign = TextAlign.Right)
        val textLayoutResult = textMeasure.measure(text = String.format("%.2f x %.2f %s\n%.2f cm\u00B2", w, h, unit.value, w*h), style = style)
        val textSize = textLayoutResult.size
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = size.width - textSize.width - 20.dp.toPx(),
                y = 10f
            ),
            color = txtColor

        )
        // draw measure rectangle
        drawRect(color = color, size = dim.value, topLeft = Offset(x = center.value.x - dim.value.width/2f, y = center.value.y - dim.value.height/2f))
    }
}

@Composable
private fun ShowSettingDialog(currFactor: MutableState<Float>, currUnit: MutableState<String>, showDialog: MutableState<Boolean>, dpi: Int) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val keyCalibration = stringResource(R.string.correction_factor)
    val keyUnit = stringResource(R.string.unit)
    val prevCalibration = preferencesManager.getData(keyCalibration, 1f.toString()).toFloat()
    val prevUnit = preferencesManager.getData(keyUnit, "cm")

    AlertDialog(
        onDismissRequest = {
            currFactor.value = prevCalibration
            currUnit.value = prevUnit
            showDialog.value = false
        },
        title = { Text(text = "Calibration") },
        text = {
            Column (horizontalAlignment = Alignment.End, modifier = Modifier.padding(0.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100f.dp)
                ) {
                    DrawTicks(dpi = dpi, vertical = false, factor = currFactor, unit = currUnit)
                    DrawLabels(dpi = dpi, vertical = false, factor = currFactor, unit = currUnit)
                }
                Slider(
                    value = currFactor.value,
                    onValueChange = { currFactor.value = it },
                    valueRange = 0.6f..1.4f
                )
                Row(
                    Modifier
                        .padding(0.dp)
                        .wrapContentHeight()) {
                    Text(text = String.format("Calibration Factor: %.2f", currFactor.value), fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    IconButton(onClick = { currFactor.value = 1f }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reset",
                            Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row {
                    Text(text = String.format("Unit: %s", currUnit.value), fontSize = 18.sp, modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(10.dp, 0.dp))

                    Switch(
                        checked = currUnit.value == "cm",
                        onCheckedChange = {
                            currUnit.value = if (it) "cm" else "inch"
                        }
                    )
                }
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    currFactor.value = prevCalibration
                    currUnit.value = prevUnit
                    showDialog.value = false
                }
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    preferencesManager.saveData(keyCalibration, currFactor.value.toString())
                    preferencesManager.saveData(keyUnit, currUnit.value)
                    showDialog.value = false
                }
            ) {
                Text("Save")
            }
        }
    )
}

/*@Preview(showBackground = true)
@Composable
fun RulerPreview() {
    BasicRulerTheme {
        val factor = remember { mutableStateOf(1f) }
        val showDialog = remember { mutableStateOf(true) }
        val unit = remember { mutableStateOf("cm") }
        DrawTicks(420, tickLength = 30, factor = factor, vertical = true, unit = unit)
        DrawLabels(420, vertical = true, factor = factor, tickLength = 30, unit = unit)
        DrawOptions(factor = factor, dpi = 420, unit = unit)
        //Dialog(currFactor = factor, currUnit = unit, dpi = 420, showDialog =  showDialog)
    }
}*/
