package com.joecode.brokemon.ui.group

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.GroupPhoto
import com.joecode.brokemon.domain.PhotoBackdrop
import com.joecode.brokemon.domain.PhotoPose
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.AvatarBitmaps
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.DexySays
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.PixelChip
import com.joecode.brokemon.ui.components.PixelPanel
import com.joecode.brokemon.ui.feedback.LocalFeedback
import com.joecode.brokemon.ui.share.ShareImage
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GroupPhotoOptions(val bros: List<Bro> = emptyList(), val preselected: List<Long> = emptyList())

class GroupPhotoViewModel(savedStateHandle: SavedStateHandle, repository: BroRepository) : ViewModel() {
    private val squadId: Long = savedStateHandle.get<Long>("squadId") ?: 0L
    val options: StateFlow<GroupPhotoOptions> = combine(repository.bros, repository.squads) { bros, squads ->
        val members = squads.firstOrNull { it.id == squadId }?.memberIds.orEmpty()
        GroupPhotoOptions(bros, (members.ifEmpty { bros.take(3).map { it.id } }).take(GroupPhoto.MAX_BROS))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupPhotoOptions())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupPhotoScreen(onBack: () -> Unit, viewModel: GroupPhotoViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    var picked by rememberSaveable { mutableStateOf<List<Long>?>(null) }
    var backdrop by rememberSaveable { mutableStateOf(PhotoBackdrop.SKY) }
    var pose by rememberSaveable { mutableStateOf(PhotoPose.LINE) }
    val selectedIds = picked ?: options.preselected
    val selected = options.bros.filter { it.id in selectedIds }.sortedBy { selectedIds.indexOf(it.id) }

    val pixels = remember(selected.map { it.id to it.resolvedLook }, backdrop, pose) { GroupPhoto.render(selected, backdrop, pose) }
    val bitmap = remember(pixels) { Bitmap.createBitmap(pixels, GroupPhoto.W, GroupPhoto.H, Bitmap.Config.ARGB_8888) }

    DexScaffold(title = "Group photo", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (options.bros.isEmpty()) {
                DexySays("Catch a few bros first, then come back for a group photo!")
                return@Column
            }
            Image(
                bitmap.asImageBitmap(), "Group photo preview of ${selected.joinToString { it.name }}",
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(GroupPhoto.W / GroupPhoto.H.toFloat())
                    .border(3.dp, DexColors.Outline),
                contentScale = ContentScale.FillBounds,
                filterQuality = AvatarBitmaps.filterQuality,
            )
            PixelPanel(title = "Who's in it? (${selected.size}/${GroupPhoto.MAX_BROS})", modifier = Modifier.fillMaxWidth()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    options.bros.forEach { bro ->
                        val on = bro.id in selectedIds
                        Box(
                            Modifier
                                .size(64.dp)
                                .semantics { contentDescription = "${bro.name}, ${if (on) "in the photo" else "not in the photo"}" }
                                .border(if (on) 3.dp else 1.dp, if (on) DexColors.LedYellow else DexColors.Outline)
                                .clickable(role = androidx.compose.ui.semantics.Role.Checkbox) {
                                    picked = if (on) selectedIds - bro.id else if (selectedIds.size < GroupPhoto.MAX_BROS) selectedIds + bro.id else selectedIds
                                },
                            contentAlignment = Alignment.Center,
                        ) { BroSprite(bro, 0, Modifier.size(56.dp)) }
                    }
                }
            }
            PixelPanel(title = "Backdrop", modifier = Modifier.fillMaxWidth()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PhotoBackdrop.entries.forEach { PixelChip(it.label, it == backdrop, { backdrop = it }) }
                }
            }
            PixelPanel(title = "Pose", modifier = Modifier.fillMaxWidth()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PhotoPose.entries.forEach { PixelChip(it.label, it == pose, { pose = it }) }
                }
            }
            PixelButton(
                "Share photo",
                onClick = {
                    // 6x nearest-neighbor: a crisp 720x1284 story-sized image.
                    val big = Bitmap.createScaledBitmap(bitmap, GroupPhoto.W * 6, GroupPhoto.H * 6, false)
                    ShareImage.share(context, big, "brokemon_group_photo", "Share group photo")
                    feedback?.success("Photo ready")
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Only the picture leaves the phone, and only when you tap Share.", color = DexColors.TextMuted, style = PixelText.Tiny)
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
