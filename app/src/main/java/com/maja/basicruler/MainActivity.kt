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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maja.basicruler.ui.theme.BasicRulerTheme
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dpi = this.resources.configuration.densityDpi.takeIf { it != DENSITY_DPI_UNDEFINED } ?: 240

        setContent {
            BasicRulerTheme {
                val context = LocalContext.current
                val preferencesManager = remember { PreferencesManager(context) }
                val prevCalibration = preferencesManager.getData(stringResource(R.string.correction_factor), stringResource(R.string.default_calibration_value)).toFloat()
                val currCalibration = remember { mutableStateOf(prevCalibration) }
                val prevUnit = preferencesManager.getData(stringResource(R.string.unit), stringResource(R.string.default_unit))
                val currUnit = remember { mutableStateOf(prevUnit) }

                Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    DrawTicks(dpi = dpi, factor = currCalibration, unit = currUnit)
                    DrawOptions(dpi = dpi, factor = currCalibration, unit = currUnit)
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DrawTicks(dpi: Int, tickLength: Int = 30, vertical: Boolean = true, factor: MutableState<Float>, unit: MutableState<String>, offsetX: Float = 0f, offsetY: Float = 10f) {
    val colorRect = MaterialTheme.colorScheme.primary
    val colorTicks = MaterialTheme.colorScheme.background
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize
    val textMeasure = rememberTextMeasurer()
    val tickLength1 = tickLength * 0.7f
    val tickLength5 = tickLength * 0.8f
    val unitDist = if (unit.value == stringResource(R.string.default_unit)) 2.54f else 1f
    val mm = dpi.toFloat() * (1f / unitDist) / 10f * factor.value
   // val portrait_mode = Configuration.ORIENTATION_PORTRAIT == LocalConfiguration.current.orientation

    Canvas(modifier = Modifier.fillMaxWidth()) {
        val strokeWidthMain = (tickLength / 20).dp.toPx()
        val strokeWidthIntermediate = (tickLength / 30).dp.toPx()
        val rectWidth = (tickLength * 2.5f).dp.toPx()

        val mmNum = if (vertical) (size.height / mm).toInt() else (size.width / mm).toInt()
        val rect = if (vertical) Size(rectWidth, size.height) else Size(size.width, rectWidth)

        drawRect(color = colorRect, size = rect)

        for (distance in 0..mmNum) {
            val y = mm * distance + offsetY
            var start = Offset(x = offsetX, y = y)
            var end: Offset
            var stroke: Float

            if (distance % 10 == 0) {
                end = start.copy(x = offsetX + tickLength.dp.toPx())
                stroke = strokeWidthMain
                // Draw the label
                val textLayoutResult = textMeasure.measure(
                    text = AnnotatedString((distance / 10).toString()),
                    style = TextStyle(fontSize = fontSize, color = colorTicks)
                )
                val textSize = textLayoutResult.size
                val topLeft = if (vertical) {
                    Offset(-0.5f * textSize.width + distance * mm + offsetY, -0.5f * textSize.height - (1.5f * tickLength).dp.toPx())
                } else {
                    Offset(distance * mm + offsetY - 0.5f * textSize.width, (1.5f * tickLength).dp.toPx() - 0.5f * textSize.height)
                }
                if (vertical) {
                    rotate(degrees = 90f, pivot = Offset(x = 0f, y = 0f)) {
                        drawText(textLayoutResult = textLayoutResult, topLeft = topLeft)
                    }
                } else {
                    drawText(textLayoutResult = textLayoutResult, topLeft = topLeft)
                }
            } else if (distance % 5 == 0) {
                end = start.copy(x = offsetX + tickLength5.dp.toPx())
                stroke = strokeWidthIntermediate
            } else {
                end = start.copy(x = offsetX + tickLength1.dp.toPx())
                stroke = strokeWidthIntermediate
            }

            if (!vertical) {
                end = Offset(x = end.y, y = end.x)
                start = Offset(x = start.y, y = start.x)
            }
            drawLine(start = start, end = end, color = colorTicks, strokeWidth = stroke)
        }
    }
}

@Composable
fun DrawOptions(dpi: Int, factor: MutableState<Float>, unit: MutableState<String>) {
    val showSetting = remember { mutableStateOf(false) }
    val showSquareMeasure = remember { mutableStateOf(false) }
    val showLinealMeasure = remember { mutableStateOf(false) }
    // draw this on the bottom layer
    if (showSquareMeasure.value) {
        ShowMeasure(factor, unit, dpi)
    }
    if (showLinealMeasure.value) {
        ShowLinealMeasure(factor, unit, dpi)
    }
    if (showSetting.value) {
        ShowSettingDialog(factor, unit, showSetting, dpi)
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
        MeasureButton(
            imageVector = ImageVector.vectorResource(id = R.drawable.lineal_meas),
            isSelected = showLinealMeasure.value,
            onClick = {
                showLinealMeasure.value = !showLinealMeasure.value
                showSquareMeasure.value = false
            }
        )
        MeasureButton(
            imageVector = ImageVector.vectorResource(id = R.drawable.square_meas),
            isSelected = showSquareMeasure.value,
            onClick = {
                showSquareMeasure.value = !showSquareMeasure.value
                showLinealMeasure.value = false
            }
        )
        MeasureButton(
            imageVector = Icons.Default.Settings,
            isSelected = showSetting.value,
            onClick = { showSetting.value = true }
        )
    }
}

@Composable
fun MeasureButton(imageVector: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val buttonColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        modifier = Modifier.padding(10.dp).size(60.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        Icon(imageVector, contentDescription = "Measure", Modifier.size(40.dp), tint = MaterialTheme.colorScheme.background)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ShowMeasure(factor: MutableState<Float>, unit: MutableState<String>, dpi: Int) {
    val color = MaterialTheme.colorScheme.secondary.copy(0.85f)
    val defaultUnit = stringResource(R.string.default_unit)
    val unitDist = if (unit.value == defaultUnit) 2.54f else 1f
    val offset = dpi.toFloat() * (1f / unitDist) * factor.value
    val offsetY = 10f
    val txtColor = MaterialTheme.colorScheme.primary
    val measDimension = if (unit.value == defaultUnit) 2f * offset else offset
    val dim = remember { mutableStateOf(Size(measDimension, measDimension)) }
    val center = remember { mutableStateOf(Offset(measDimension + 0.5f * dim.value.width, measDimension + offsetY + 0.5f * dim.value.height)) }
    val textMeasure = rememberTextMeasurer()
    val fontSize = MaterialTheme.typography.labelLarge.fontSize

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, delta ->
                val currPos = change.position
                // check for close center this enables drag&drop
                val radius = sqrt(
                    (center.value.x - currPos.x).pow(2) + (center.value.y - currPos.y).pow(2)
                )
                val resizeThreshold = min(100f, 0.5f * (dim.value.width + dim.value.height))

                if (radius < resizeThreshold) {
                    center.value += delta
                }
                else if (abs(delta.x) > abs(delta.y)) { // movement is in x-direction
                    val sign = if (currPos.x > center.value.x) 1.0f else -1.0f
                    center.value = center.value.copy(x = center.value.x + 0.5f * delta.x)
                    dim.value = dim.value.copy(width = dim.value.width + sign * delta.x)
                } else { // movement is in y direction
                    val sign = if (currPos.y < center.value.y) -1.0f else 1.0f
                    center.value = center.value.copy(y = center.value.y + 0.5f * delta.y)
                    dim.value = dim.value.copy(height = dim.value.height + sign * delta.y)
                }
            }
        }) {
        // Calculate the screen limits of the rectangle
        val dXR = size.width - (center.value.x + dim.value.width / 2f)
        val dXL = center.value.x - dim.value.width / 2f
        val dYT = center.value.y - dim.value.height / 2f
        val dYB = size.height - (center.value.y + dim.value.height / 2f)

        // Adjust position and size to stay within screen bounds
        if (dXR < 0) {
            center.value = center.value.copy(x = center.value.x - abs(dXR) / 2f)
            dim.value = dim.value.copy(width = dim.value.width - abs(dXR))
        }
        if (dXL < 0) {
            center.value = center.value.copy(x = center.value.x + abs(dXL) / 2f)
            dim.value = dim.value.copy(width = dim.value.width - abs(dXL))
        }
        if (dYT < 0) {
            center.value = center.value.copy(y = center.value.y + abs(dYT) / 2f)
            dim.value = dim.value.copy(height = dim.value.height - abs(dYT))
        }
        if (dYB < 0) {
            center.value = center.value.copy(y = center.value.y - abs(dYB) / 2f)
            dim.value = dim.value.copy(height = dim.value.height - abs(dYB))
        }

        // draw size of rectangle
        val w = dim.value.width / offset
        val h = dim.value.height / offset
        val textLayoutResult = textMeasure.measure(
            text = String.format("%.2f x %.2f %s\n%.2f %s\u00B2", w, h, unit.value, w*h, unit.value),
            style = TextStyle(fontSize = fontSize, color = color, textAlign = TextAlign.Right))
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = size.width - textLayoutResult.size.width - 20.dp.toPx(),
                y = 10f
            ),
            color = txtColor

        )
        // draw measure rectangle
        drawRect(color = color, size = dim.value, topLeft = Offset(center.value.x - 0.5f * dim.value.width, center.value.y - 0.5F * dim.value.height))
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ShowLinealMeasure(factor: MutableState<Float>, unit: MutableState<String>, dpi: Int) {
    val color = MaterialTheme.colorScheme.secondary.copy(0.85f)
    val unitDist = if (unit.value == stringResource(R.string.default_unit)) 2.54f else 1f
    val offset = dpi.toFloat() * (1f / unitDist) * factor.value
    val offsetY = 10f
    val txtColor = MaterialTheme.colorScheme.primary
    val measDimension = if (unit.value == stringResource(R.string.default_unit)) 2f*offset else offset
    val dim = remember { mutableStateOf(Size(width = measDimension, height = measDimension)) }
    val center = remember { mutableStateOf(Offset(x = measDimension + dim.value.width/2f, y = measDimension+offsetY + dim.value.height/2f)) }
    val textMeasure = rememberTextMeasurer()
    val fontSize = MaterialTheme.typography.labelLarge.fontSize

    Canvas(modifier = Modifier
        .fillMaxSize()
        .onSizeChanged {
            dim.value = Size(width = it.width.toFloat(), height = dim.value.height)
            center.value = Offset(x = dim.value.width / 2f, y = center.value.y)
        }
        .pointerInput(key1 = Unit) {
            detectDragGestures(
                onDrag = { change, delta ->
                    val currPos = change.position
                    if (abs(delta.x) < abs(delta.y)) { // movement is in y direction
                        // resize rectangle on top side
                        val sign = if (currPos.y < center.value.y) -1.0f else 1.0f
                        center.value = center.value.copy(y = center.value.y + 0.5f * delta.y)
                        dim.value = dim.value.copy(height = dim.value.height + sign * delta.y)
                    }
                }
            )
        }) {
        // if negative the rect is overhanging on the top side
        val dYT = center.value.y - 0.5f * dim.value.height
        if (dYT < 0) {
            center.value = Offset(x = center.value.x, y = center.value.y + abs(dYT)/2f)
            dim.value = Size(width = dim.value.width, height = dim.value.height - abs(dYT))
        }
        // if negative the rect is overhanging on the bottom side
        val dYB = size.height - (center.value.y + 0.5f * dim.value.height)
        if (dYB < 0) {
            center.value = Offset(x = center.value.x, y = center.value.y - abs(dYB)/2f)
            dim.value = Size(width = dim.value.width, height = dim.value.height - abs(dYB))
        }

        // draw size of rectangle
        val textLayoutResult = textMeasure.measure(
            text = String.format("%.2f %s", dim.value.height / offset, unit.value),
            style = TextStyle(fontSize = fontSize, color = color, textAlign = TextAlign.Right))
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
    val prevUnit = preferencesManager.getData(keyUnit, stringResource(R.string.default_unit))
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize
    val buttonFontSize = MaterialTheme.typography.labelMedium.fontSize

    AlertDialog(
        onDismissRequest = {
            currFactor.value = prevCalibration
            currUnit.value = prevUnit
            showDialog.value = false
        },
        title = { Text(text = "Calibration") },
        text = {
            Column (horizontalAlignment = Alignment.End, modifier = Modifier.padding(0.dp)) {
                val tickLength = 30
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((tickLength * 2.5f).dp)
                ) {
                    DrawTicks(dpi = dpi, tickLength = tickLength, vertical = false, factor = currFactor, unit = currUnit)
                }
                Slider(
                    modifier = Modifier.padding(0.dp),
                    value = currFactor.value,
                    onValueChange = { currFactor.value = it },
                    valueRange = 0.6f..1.4f
                )
                Row(
                    Modifier
                        .padding(0.dp)
                        .wrapContentHeight()) {
                    Text(text = String.format("Calibration Factor: %.2f", currFactor.value), fontSize = fontSize, modifier = Modifier.align(Alignment.CenterVertically))
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
                    Text(text = String.format("Unit: %s", currUnit.value), fontSize = fontSize, modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(10.dp, 0.dp))

                    Switch(
                        checked = currUnit.value == prevUnit,
                        onCheckedChange = {
                            currUnit.value = if (it) prevUnit else "inch"
                        }
                    )
                }
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier.fillMaxWidth(0.4f),
                onClick = {
                    currFactor.value = prevCalibration
                    currUnit.value = prevUnit
                    showDialog.value = false
                }
            ) {
                Text("Cancel", fontSize = buttonFontSize)
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth(0.4f),
                onClick = {
                    preferencesManager.saveData(keyCalibration, currFactor.value.toString())
                    preferencesManager.saveData(keyUnit, currUnit.value)
                    showDialog.value = false
                }
            ) {
                Text("Save", fontSize = buttonFontSize)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun RulerPreview() {
    BasicRulerTheme {
        val factor = remember { mutableStateOf(1f) }
        val showDialog = remember { mutableStateOf(true) }
        val unit = remember { mutableStateOf("cm") }
        DrawTicks(420, tickLength = 30, factor = factor, vertical = true, unit = unit)
        DrawOptions(factor = factor, dpi = 420, unit = unit)
        ShowSettingDialog(currFactor = factor, currUnit = unit, dpi = 420, showDialog =  showDialog)
        ShowLinealMeasure(factor = factor, unit = unit, dpi = 420)
    }
}
