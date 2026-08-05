package com.af.synapse.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.CpuManager
import com.af.synapse.data.GpuManager
import com.af.synapse.data.GenericManager
import com.af.synapse.ui.theme.PixelBlue

@Composable
fun SummaryScreen() {
    val scrollState = rememberScrollState()
    
    val deviceModel = android.os.Build.MODEL
    val manufacturer = android.os.Build.MANUFACTURER.uppercase()
    
    // SoC Name detection (Tensor G4, etc.)
    val socName = remember {
        var detected: String? = null
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            try {
                val field = android.os.Build::class.java.getField("SOC_MODEL")
                detected = field.get(null) as? String
            } catch (e: Exception) { }
        }
        
        if (detected == null || detected.isEmpty()) {
            detected = GenericManager.readFile("/proc/cpuinfo")
                .lines()
                .find { it.startsWith("Hardware") }
                ?.substringAfter(":")?.trim()
        }

        when {
            detected?.contains("gs301", true) == true -> "Google Tensor G3"
            detected?.contains("gs201", true) == true -> "Google Tensor G2"
            detected?.contains("gs101", true) == true -> "Google Tensor"
            detected?.contains("gs401", true) == true || detected?.contains("tensor g4", true) == true -> "Google Tensor G4"
            else -> detected ?: "Octa-Core Processor"
        }
    }

    // ROM Info
    val buildType = remember {
        val props = GenericManager.readFile("/system/build.prop")
        val isLineage = props.contains("lineage", true) || GenericManager.exists("/system/addon.d/50-lineage.sh")
        val isPixel = props.contains("google", true) && props.contains("pixel", true)
        
        when {
            isLineage -> "LineageOS"
            isPixel -> "Google"
            else -> "Stock/Custom"
        }
    }
        
    val compilation = android.os.Build.DISPLAY
    val androidVer = android.os.Build.VERSION.RELEASE
    val kernelVer = System.getProperty("os.version")?.split("-")?.firstOrNull() ?: "Unknown"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header: Big Device Name and Chip Image
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                Text(
                    text = manufacturer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = PixelBlue,
                    letterSpacing = 2.sp
                )
                Text(
                    text = deviceModel,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 38.sp
                )
            }
            
            Image(
                painter = painterResource(id = R.drawable.ic_chip_custom),
                contentDescription = "Chipset",
                modifier = Modifier.size(110.dp)
            )
        }

        // 2. Processor Section
        SummaryDashboardSection(title = stringResource(R.string.summary_cpu)) {
            val clusters = CpuManager.getAvailableClusters()
            var totalCores = 0
            clusters.forEach { totalCores += CpuManager.getClusterCpus(it).size }
            
            val coreCountText = when(totalCores) {
                4 -> "Quad-core Processor"
                6 -> "Hexa-core Processor"
                8 -> "Octa-core Processor"
                10 -> "Deca-core Processor"
                else -> "$totalCores-core Processor"
            }

            Text(
                text = socName,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = coreCountText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            clusters.forEachIndexed { index, i ->
                val cpus = CpuManager.getClusterCpus(i)
                val label = when(index) {
                    0 -> stringResource(R.string.nav_cpu_silver)
                    1 -> stringResource(R.string.nav_cpu_gold)
                    2 -> stringResource(R.string.nav_cpu_perf)
                    else -> "Cluster $index"
                }
                SummaryDashboardRow(label, "Cores ${cpus.first()}-${cpus.last()}")
            }
        }

        // 3. GPU Section
        SummaryDashboardSection(title = stringResource(R.string.summary_gpu)) {
            val gpuPath = GpuManager.getGpuPath() ?: ""
            
            val gpuModelName = when {
                socName.contains("G4") || gpuPath.contains("1f000000.mali") -> "ARM Mali-G715 (Immortalis)"
                socName.contains("G3") || gpuPath.contains("1c500000.mali") -> "ARM Mali-G715"
                socName.contains("gs201") -> "ARM Mali-G710"
                gpuPath.contains("mali") -> "ARM Mali™ Graphics"
                gpuPath.contains("kgsl") -> {
                    val model = GenericManager.readFile("/sys/class/kgsl/kgsl-3d0/gpu_model").trim()
                    if (model.isNotEmpty()) "Adreno (TM) $model" else "Qualcomm Adreno™"
                }
                else -> "Integrated GPU"
            }
            
            Text(
                text = gpuModelName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val renderer = if (gpuPath.contains("mali")) "Mali Graphics" else "Adreno Graphics"
            SummaryDashboardRow("Renderer", renderer)
        }

        // 4. ROM / System Details Section
        SummaryDashboardSection(title = stringResource(R.string.header_rom)) {
            SummaryDashboardRow(stringResource(R.string.summary_build_type), buildType)
            SummaryDashboardRow(stringResource(R.string.summary_compilation), compilation)
            SummaryDashboardRow(stringResource(R.string.summary_android_ver), androidVer)
            SummaryDashboardRow(stringResource(R.string.summary_kernel_ver), kernelVer)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SummaryDashboardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = PixelBlue,
            letterSpacing = 1.5.sp
        )
        
        Surface(
            color = surfaceColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SummaryDashboardRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
