package com.example.navigation

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

// data model for the user financial journey.
data class BudgetMilestone(
    val id: Int,
    val title: String,
    val requiredXP: Int,
    val status: MilestoneStatus
)

//milestone statuses.
enum class MilestoneStatus {
    COMPLETED,
    CURRENT,
    LOCKED
}

class ProgressActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val TAG = "ProgressActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Progress Journey")
        
        // standard edge-to-edge setup for a modern look.
        enableEdgeToEdge()
        setContentView(R.layout.activity_progress)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationSidebar = findViewById<NavigationView>(R.id.nav_view)
        val menuIcon = findViewById<ImageView>(R.id.btn_menu)

        // Opening the side menu - a classic UI pattern for secondary navigation.
        menuIcon.setOnClickListener {
            Log.d(TAG, "Side menu opened")
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handling sidebar clicks. We use no-animation flags to make transitions feel snappy.
        navigationSidebar.setNavigationItemSelectedListener { item ->
            Log.d(TAG, "Navigation item selected: ${item.title}")
            val destination = when (item.itemId) {
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                R.id.nav_reports -> Intent(this, ReportActivity::class.java)
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> Intent(this, ManualActivity::class.java)
                R.id.nav_logout -> {
                    performLogout()
                    null
                }
                else -> null
            }
            
            destination?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(it)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Adjusting for system bars so the UI doesn't get clipped by notches or nav bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        setupBottomNavigation()

        // Retrieving the user's progress. We store this in "UserData" prefs.
        val composeContainer = findViewById<ComposeView>(R.id.compose_view)
        val userStats = getSharedPreferences("UserData", MODE_PRIVATE)
        val userTotalXP = userStats.getInt("xp", 0)
        
        Log.d(TAG, "Current User XP: $userTotalXP")
        
        // Keep the cloud in sync with the local progress.
        syncXPWithFirebase(userTotalXP)

        // Here we bridge the gap between traditional XML layouts and Jetpack Compose.
        composeContainer.setContent {
            val bgColor = colorResource(id = R.color.background_gray)
            val primaryBlue = colorResource(id = R.color.primary_blue)
            val surfaceWhite = colorResource(id = R.color.surface_light)
            
            MaterialTheme {
                Surface(color = bgColor) {
                    // Defining the tiers of our financial hierarchy.
                    val milestoneConfigs = listOf(
                        Triple(1, stringResource(id = R.string.label_generational_wealth_builder), 1000),
                        Triple(2, stringResource(id = R.string.label_wealth_builder), 500),
                        Triple(3, stringResource(id = R.string.label_strategist), 200),
                        Triple(4, stringResource(id = R.string.label_saver), 100),
                        Triple(5, stringResource(id = R.string.label_financial_novice), 10)
                    )

                    // Calculating which milestones are done, active, or locked based on current XP.
                    val milestonesToDisplay = milestoneConfigs.map { (id, name, threshold) ->
                        val status = when {
                            userTotalXP >= threshold -> MilestoneStatus.COMPLETED
                            milestoneConfigs.sortedBy { it.third }.firstOrNull { it.third > userTotalXP }?.first == id -> MilestoneStatus.CURRENT
                            else -> MilestoneStatus.LOCKED
                        }
                        BudgetMilestone(id, name, threshold, status)
                    }.sortedBy { it.requiredXP }
                    
                    // Rendering the visual path map.
                    BudgetTrackingMap(
                        userTotalXP = userTotalXP,
                        milestones = milestonesToDisplay,
                        accentBlue = primaryBlue,
                        cardBg = surfaceWhite,
                        onMilestoneClick = { clickedMilestone ->
                            Log.d(TAG, "Milestone clicked: ${clickedMilestone.title}")
                            Toast.makeText(this@ProgressActivity, 
                                "Target: ${clickedMilestone.title} at ${clickedMilestone.requiredXP} XP", 
                                Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    /**
     * Ensures that the user's hard-earned XP is backed up to Firestore.
     * This allows progress to persist across devices.
     */
    private fun syncXPWithFirebase(localXP: Int) {
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val email = userPrefs.getString("REG_EMAIL", null) ?: return
        
        Log.d(TAG, "Syncing XP to Firestore for $email")
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(email)

        userDoc.set(mapOf("xp" to localXP), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Firestore XP sync successful")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore XP sync failed", e)
            }
    }

    /**
     * Standard bottom navigation setup. We kill the activity on switch to prevent
     * a massive backstack of activities from building up.
     */
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_progress
        
        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "Bottom Nav selected: ${item.itemId}")
            when (item.itemId) {
                R.id.nav_home -> navigateTo(ThirdActivity::class.java)
                R.id.nav_add -> navigateTo(BudgetActivity::class.java)
                R.id.nav_progress -> true
                R.id.nav_add_expense -> navigateTo(ForthActivity::class.java)
                else -> false
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) : Boolean {
        val intent = Intent(this, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish() // important to keep the app flow linear and memory usage low.
        return true
    }

    private fun performLogout() {
        Log.d(TAG, "Logging out user")
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userPrefs.edit().putBoolean("IS_LOGGED_IN", false).apply()
        
        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

/**
 * The core visual component of the Progress screen.
 * It draws an S-curve path representing the user's financial journey.
 */
@Composable
fun BudgetTrackingMap(
    userTotalXP: Int,
    milestones: List<BudgetMilestone>,
    accentBlue: Color,
    cardBg: Color,
    onMilestoneClick: (BudgetMilestone) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Auto-scroll to the bottom (start of journey) on first load.
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    val disabledGray = colorResource(id = R.color.gray_light)
    val textPrimary = colorResource(id = R.color.text_primary)
    val bgColor = colorResource(id = R.color.background_gray)
    val successGreen = colorResource(id = R.color.success_green)

    Column(modifier = modifier.fillMaxSize().background(bgColor)) {
        // Enhanced Header Status Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Financial Rank", fontSize = 12.sp, color = Color.Gray)
                        val currentMilestone = milestones.lastOrNull { it.status == MilestoneStatus.COMPLETED }?.title ?: "Beginner"
                        Text(currentMilestone, fontWeight = FontWeight.ExtraBold, color = textPrimary, fontSize = 20.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(accentBlue.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("$userTotalXP XP", fontWeight = FontWeight.Bold, color = accentBlue, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val nextMilestone = milestones.firstOrNull { it.status != MilestoneStatus.COMPLETED }
                if (nextMilestone != null) {
                    val progressToNext = (userTotalXP.toFloat() / nextMilestone.requiredXP).coerceIn(0f, 1f)
                    Text(
                        text = stringResource(id = R.string.xp_status_placeholder, nextMilestone.requiredXP - userTotalXP),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = { progressToNext },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = accentBlue,
                        trackColor = disabledGray.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    Text(stringResource(id = R.string.xp_max_tier), fontWeight = FontWeight.Bold, color = successGreen)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            val pathHeight = 1000.dp 

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(pathHeight)) {
                val w = size.width
                val h = size.height

                // Smoother S-curve path
                val trackPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.95f)
                    cubicTo(w * 0.05f, h * 0.8f, w * 0.95f, h * 0.65f, w * 0.5f, h * 0.5f)
                    cubicTo(w * 0.05f, h * 0.35f, w * 0.95f, h * 0.2f, w * 0.5f, h * 0.05f)
                }

                val progressRatio = (userTotalXP.toFloat() / 1000f).coerceIn(0f, 1f)
                
                // 1. Draw dashed background track
                drawPath(
                    path = trackPath,
                    color = disabledGray.copy(alpha = 0.5f),
                    style = Stroke(
                        width = 8.dp.toPx(), 
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )
                )

                // 2. Draw active solid track
                drawPath(
                    path = trackPath,
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        (1f - progressRatio).coerceIn(0f, 1f) to Color.Transparent,
                        (1f - progressRatio).coerceIn(0f, 1f) to accentBlue,
                        1.0f to accentBlue,
                        startY = h * 0.05f,
                        endY = h * 0.95f
                    ),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pathHeight)
            ) {
                milestones.forEach { milestone ->
                    val milestoneXP = milestone.requiredXP.toFloat()
                    val ratio = (milestoneXP / 1000f)
                    
                    val yPos = (0.95f - ratio * 0.9f) * pathHeight.value
                    
                    val xOffset = when {
                        milestoneXP <= 50 -> 0.dp
                        milestoneXP <= 300 -> (-110).dp 
                        milestoneXP <= 500 -> 0.dp      
                        milestoneXP <= 800 -> 110.dp     
                        else -> 0.dp                     
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = xOffset, y = yPos.dp - 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MilestoneNode(
                            milestone = milestone,
                            activeColor = accentBlue,
                            surfaceColor = cardBg,
                            textPrimary = textPrimary,
                            onClick = { 
                                if (milestone.status != MilestoneStatus.LOCKED) onMilestoneClick(milestone) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneNode(
    milestone: BudgetMilestone,
    activeColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    onClick: () -> Unit
) {
    val statusColor = when (milestone.status) {
        MilestoneStatus.COMPLETED -> activeColor
        MilestoneStatus.CURRENT -> colorResource(id = R.color.success_green)
        MilestoneStatus.LOCKED -> colorResource(id = R.color.gray_medium)
    }

    val isPremium = milestone.requiredXP >= 500
    val premiumColor = if (milestone.requiredXP >= 1000) colorResource(id = R.color.premium_gold) else colorResource(id = R.color.premium_silver)

    val icon = when (milestone.requiredXP) {
        10 -> Icons.Default.ShoppingCart // Novice
        100 -> Icons.Default.Build // Saver
        200 -> Icons.Default.LocationOn // Strategist
        500 -> Icons.Default.Star // Wealth Builder
        1000 -> Icons.Default.KeyboardArrowUp // Generational
        else -> Icons.Default.CheckCircle
    }

    // Floating animation for "Current" milestone
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (milestone.status == MilestoneStatus.CURRENT) -10f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
            .graphicsLayer { translationY = dy }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (milestone.status == MilestoneStatus.CURRENT) statusColor.copy(alpha = 0.2f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(if (isPremium) 8.dp else 2.dp, CircleShape)
                    .background(color = surfaceColor, shape = CircleShape)
                    .then(
                        if (isPremium && milestone.status != MilestoneStatus.LOCKED)
                            Modifier.border(2.dp, premiumColor, CircleShape)
                        else if (milestone.status != MilestoneStatus.LOCKED)
                            Modifier.background(statusColor.copy(alpha = 0.1f), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (milestone.status == MilestoneStatus.LOCKED) Icons.Default.Lock else icon,
                    contentDescription = "Status Icon",
                    tint = if (isPremium && milestone.status != MilestoneStatus.LOCKED) premiumColor else statusColor,
                    modifier = Modifier.size(28.dp).alpha(if (milestone.status == MilestoneStatus.LOCKED) 0.5f else 1f)
                )

                if (milestone.status == MilestoneStatus.LOCKED && isPremium) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorResource(id = R.color.locked_overlay).copy(alpha = 0.1f), CircleShape)
                    )
                }
            }
            
            // Premium Badge
            if (isPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(premiumColor, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isPremium && milestone.status != MilestoneStatus.LOCKED) premiumColor.copy(alpha = 0.1f) else surfaceColor
            ),
            elevation = CardDefaults.cardElevation(if (milestone.status == MilestoneStatus.CURRENT) 6.dp else 2.dp),
            shape = RoundedCornerShape(12.dp),
            border = if (milestone.status == MilestoneStatus.CURRENT) BorderStroke(1.dp, statusColor) else null
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = milestone.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 13.sp
                )
                Text(
                    text = "${milestone.requiredXP} XP",
                    fontSize = 10.sp,
                    color = if (isPremium && milestone.status != MilestoneStatus.LOCKED) premiumColor else statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBudgetTrackingMap() {
    val sampleMilestones = listOf(
        BudgetMilestone(5, "Financial Novice", 10, MilestoneStatus.COMPLETED),
        BudgetMilestone(4, "Saver", 100, MilestoneStatus.CURRENT),
        BudgetMilestone(3, "Strategist", 200, MilestoneStatus.LOCKED),
        BudgetMilestone(2, "Wealth Builder", 500, MilestoneStatus.LOCKED),
        BudgetMilestone(1, "Generational Wealth Builder", 1000, MilestoneStatus.LOCKED)
    )
    
    MaterialTheme {
        BudgetTrackingMap(
            userTotalXP = 120,
            milestones = sampleMilestones,
            accentBlue = Color(0xFF1A73E8),
            cardBg = Color.White,
            onMilestoneClick = {}
        )
    }
}
