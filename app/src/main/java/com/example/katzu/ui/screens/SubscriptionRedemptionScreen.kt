package com.example.katzu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.billing.PlayBillingManager
import com.example.katzu.billing.SubscriptionPlanUi
import com.example.katzu.ui.theme.*
import com.example.katzu.util.AppLogger

@Composable
fun SubscriptionRedemptionScreen(
    userEmail: String?,
    isRedeeming: Boolean,
    errorMessage: String?,
    onRedeemCode: (code: String) -> Unit,
    onChangeAccount: () -> Unit,
    onCheckExistingSubscription: (() -> Unit)? = null,
    availablePlans: List<SubscriptionPlanUi> = emptyList(),
    isPurchasing: Boolean = false,
    onSubscribeGooglePlay: (productId: String) -> Unit = {},
    onRestorePurchases: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var showPromoCodeCard by remember { mutableStateOf(false) }

    val defaultPlans = remember {
        listOf(
            SubscriptionPlanUi(
                productId = PlayBillingManager.PRODUCT_PRO_MONTHLY,
                title = "اشتراك شهري",
                formattedPrice = "$9.99",
                billingPeriodAr = "شهرياً",
                subtitle = "محادثات غير محدودة + تصحيح فوري وتدريب النطق",
                isRecommended = false,
                savingsBadge = null
            ),
            SubscriptionPlanUi(
                productId = PlayBillingManager.PRODUCT_PRO_ANNUAL,
                title = "اشتراك سنوي",
                formattedPrice = "$59.99",
                billingPeriodAr = "سنوياً",
                subtitle = "وصول كامل لكافة المراحل والسيناريوهات طوال العام",
                isRecommended = true,
                savingsBadge = "وفر 50%"
            )
        )
    }

    val plansToDisplay = if (availablePlans.isNotEmpty()) availablePlans else defaultPlans
    var selectedProductId by remember(plansToDisplay) {
        mutableStateOf(plansToDisplay.find { it.isRecommended }?.productId ?: plansToDisplay.firstOrNull()?.productId ?: PlayBillingManager.PRODUCT_PRO_ANNUAL)
    }

    Scaffold(
        containerColor = BackgroundPure,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header & Katzu mascot
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.katzu_badge),
                    contentDescription = "كاتزو حارس البوابة",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "انضم إلى كاتزو برو (Katzu Pro)",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = Cairo,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "محادثات صوتية غير محدودة مع تصحيح نحوي دقيق وحصري من كاتزو للوصول إلى طلاقة التحدث بالألمانية.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Cairo,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                if (!userEmail.isNullOrBlank()) {
                    Surface(
                        color = SurfaceCardSubtle,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14F2F0F7))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "مرتبط بالحساب: $userEmail",
                                fontFamily = Cairo,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // PRIMARY: Google Play Subscription Plans
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "اختر خطة الاشتراك المناسبة:",
                    fontFamily = Cairo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                plansToDisplay.forEach { plan ->
                    val isSelected = plan.productId == selectedProductId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProductId = plan.productId }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Primary else Color(0x14F2F0F7),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag(if (plan.productId == PlayBillingManager.PRODUCT_PRO_ANNUAL) "plan_card_annual" else "plan_card_monthly"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SurfaceCardSubtle else SurfaceCard
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) Primary else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = plan.title,
                                        fontFamily = Cairo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                }

                                if (plan.savingsBadge != null) {
                                    Surface(
                                        color = StatusSuccess.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = plan.savingsBadge,
                                            fontFamily = Cairo,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.subtitle,
                                    fontFamily = Cairo,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = plan.formattedPrice,
                                    fontFamily = Cairo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Primary Google Play Subscription Button
                Button(
                    onClick = { onSubscribeGooglePlay(selectedProductId) },
                    enabled = !isPurchasing && !isRedeeming,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_play_subscribe_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = TextPrimary
                    )
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "الاشتراك عبر Google Play",
                                fontFamily = Cairo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Restore Purchases
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { onRestorePurchases?.invoke() },
                        enabled = !isPurchasing
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "استعادة اشتراك Google Play المسبق",
                                fontFamily = Cairo,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // SECONDARY / PROMO OPTION: Voucher & Promo Code Redemption
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x14F2F0F7), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPromoCodeCard = !showPromoCodeCard },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "لديك رمز ترويجي أو قسيمة تفعيل؟",
                                fontFamily = Cairo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }

                        Icon(
                            imageVector = if (showPromoCodeCard) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }

                    if (showPromoCodeCard) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase().trim() },
                            placeholder = {
                                Text(
                                    text = "KATZU-XXXX-XXXX",
                                    color = TextMuted,
                                    fontFamily = Cairo
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (codeInput.isNotBlank() && !isRedeeming) {
                                        onRedeemCode(codeInput)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                focusedContainerColor = SurfaceCardSubtle,
                                unfocusedContainerColor = SurfaceCardSubtle
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subscription_code_input")
                        )

                        Button(
                            onClick = {
                                if (codeInput.isNotBlank()) {
                                    onRedeemCode(codeInput)
                                }
                            },
                            enabled = codeInput.isNotBlank() && !isRedeeming,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("redeem_code_button"),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceCardSubtle,
                                contentColor = Primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                        ) {
                            if (isRedeeming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "تفعيل القسيمة والدخول",
                                    fontFamily = Cairo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = StatusError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = StatusError,
                            fontFamily = Cairo,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        if (com.example.katzu.BuildConfig.DEBUG) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { AppLogger.copyLastLinesToClipboard(context, 40) },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "نسخ تفاصيل الخطأ",
                                        fontFamily = Cairo,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { AppLogger.shareLogFile(context) },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "إرسال السجل",
                                        fontFamily = Cairo,
                                        fontSize = 11.sp,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Account and support options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onCheckExistingSubscription != null) {
                    OutlinedButton(
                        onClick = { onCheckExistingSubscription.invoke() },
                        enabled = !isRedeeming && !isPurchasing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("check_existing_subscription_button"),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "التحقق من اشتراك مرتبط بالخادم",
                                fontFamily = Cairo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onChangeAccount,
                    enabled = !isRedeeming && !isPurchasing
                ) {
                    Text(
                        text = "تسجيل الدخول بحساب Google آخر",
                        fontFamily = Cairo,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
