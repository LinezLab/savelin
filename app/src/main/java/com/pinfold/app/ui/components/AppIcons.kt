package com.pinfold.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pinfold.app.R

/**
 * The only place screens touch Lucide icons. Every name is semantic; swapping the icon set or a
 * single glyph is a one-file change. Icons are bundled as vector drawables under res/drawable.
 */
object AppIcons {
    @DrawableRes val Search = R.drawable.ic_search
    @DrawableRes val Add = R.drawable.ic_plus
    @DrawableRes val Settings = R.drawable.ic_settings
    @DrawableRes val Back = R.drawable.ic_arrow_left
    @DrawableRes val Close = R.drawable.ic_x
    @DrawableRes val More = R.drawable.ic_ellipsis
    @DrawableRes val Card = R.drawable.ic_layout_grid
    @DrawableRes val Compact = R.drawable.ic_list
    @DrawableRes val Sort = R.drawable.ic_arrow_down_up
    @DrawableRes val Filter = R.drawable.ic_list_filter
    @DrawableRes val Open = R.drawable.ic_external_link
    @DrawableRes val Browser = R.drawable.ic_globe
    @DrawableRes val Share = R.drawable.ic_share_2
    @DrawableRes val Copy = R.drawable.ic_copy
    @DrawableRes val Important = R.drawable.ic_star
    @DrawableRes val Reminder = R.drawable.ic_bell
    @DrawableRes val ReminderSet = R.drawable.ic_bell_ring
    @DrawableRes val ReminderMuted = R.drawable.ic_bell_off
    @DrawableRes val Edit = R.drawable.ic_pencil
    @DrawableRes val Delete = R.drawable.ic_trash_2
    @DrawableRes val Archive = R.drawable.ic_archive
    @DrawableRes val Restore = R.drawable.ic_archive_restore
    @DrawableRes val Refetch = R.drawable.ic_refresh_cw
    @DrawableRes val CustomImage = R.drawable.ic_image_plus
    @DrawableRes val Camera = R.drawable.ic_camera
    @DrawableRes val NoPreview = R.drawable.ic_image_off
    @DrawableRes val MoveToCollection = R.drawable.ic_folder_input
    @DrawableRes val Collection = R.drawable.ic_folder
    @DrawableRes val Tags = R.drawable.ic_tags
    @DrawableRes val Notes = R.drawable.ic_notebook_pen
    @DrawableRes val Select = R.drawable.ic_square_check_big
    @DrawableRes val Selected = R.drawable.ic_circle_check
    @DrawableRes val Paste = R.drawable.ic_clipboard_paste
    @DrawableRes val DateTime = R.drawable.ic_calendar_clock
    @DrawableRes val Snooze = R.drawable.ic_alarm_clock
    @DrawableRes val Likes = R.drawable.ic_heart
    @DrawableRes val Comments = R.drawable.ic_message_circle
    @DrawableRes val Lock = R.drawable.ic_lock
    @DrawableRes val Biometric = R.drawable.ic_fingerprint
    @DrawableRes val Hide = R.drawable.ic_eye_off
    @DrawableRes val Offline = R.drawable.ic_wifi_off
    @DrawableRes val NetworkLog = R.drawable.ic_activity
    @DrawableRes val Export = R.drawable.ic_file_down
    @DrawableRes val Import = R.drawable.ic_file_up
    @DrawableRes val Appearance = R.drawable.ic_palette
    @DrawableRes val Motion = R.drawable.ic_gauge
    @DrawableRes val Transitions = R.drawable.ic_move_horizontal
    @DrawableRes val Haptics = R.drawable.ic_vibrate
    @DrawableRes val Speed = R.drawable.ic_timer
    @DrawableRes val Light = R.drawable.ic_sun
    @DrawableRes val Dark = R.drawable.ic_moon
    @DrawableRes val Language = R.drawable.ic_languages
    @DrawableRes val Licences = R.drawable.ic_scale
    @DrawableRes val Frosted = R.drawable.ic_snowflake
    @DrawableRes val Crystal = R.drawable.ic_gem
    @DrawableRes val Liquid = R.drawable.ic_droplet
    @DrawableRes val Water = R.drawable.ic_waves
    @DrawableRes val Solid = R.drawable.ic_square
    @DrawableRes val Calm = R.drawable.ic_feather
    @DrawableRes val Normal = R.drawable.ic_gauge
    @DrawableRes val Bouncy = R.drawable.ic_chevrons_up_down
    @DrawableRes val Jelly = R.drawable.ic_blend
    @DrawableRes val GlassSpring = R.drawable.ic_droplets
    @DrawableRes val Undo = R.drawable.ic_undo_2
    @DrawableRes val Warning = R.drawable.ic_triangle_alert
    @DrawableRes val Info = R.drawable.ic_info
    @DrawableRes val Drive = R.drawable.ic_drive
    @DrawableRes val LogIn = R.drawable.ic_log_in
    @DrawableRes val KeyRound = R.drawable.ic_key_round
    @DrawableRes val LifeBuoy = R.drawable.ic_life_buoy
    @DrawableRes val History = R.drawable.ic_history
    @DrawableRes val Stethoscope = R.drawable.ic_stethoscope
    @DrawableRes val Wifi = R.drawable.ic_wifi
    @DrawableRes val Image = R.drawable.ic_image
    @DrawableRes val Clock = R.drawable.ic_clock
    @DrawableRes val Expand = R.drawable.ic_chevron_down

    @Composable
    fun Icon(
        @DrawableRes icon: Int,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        size: androidx.compose.ui.unit.Dp = 24.dp,
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint,
        )
    }
}
