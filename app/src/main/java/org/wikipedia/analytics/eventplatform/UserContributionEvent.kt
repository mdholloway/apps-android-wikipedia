package org.wikipedia.analytics.eventplatform

class UserContributionEvent(private val action: String) : Event() {

    companion object {
        fun logOpen() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("open_hist"))
        }

        fun logFilterDescriptions() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("filt_desc"))
        }

        fun logFilterCaptions() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("filt_caption"))
        }

        fun logFilterTags() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("filt_tag"))
        }

        fun logFilterAll() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("filt_all"))
        }

        fun logViewDescription() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("desc_view"))
        }

        fun logViewCaption() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("caption_view"))
        }

        fun logViewTag() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("tag_view"))
        }

        fun logViewMisc() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("misc_view"))
        }

        fun logNavigateDescription() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("desc_view2"))
        }

        fun logNavigateCaption() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("caption_view2"))
        }

        fun logNavigateTag() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("tag_view2"))
        }

        fun logNavigateMisc() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("misc_view2"))
        }

        fun logPaused() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("paused"))
        }

        fun logDisabled() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("disabled"))
        }

        fun logIpBlock() {
            EventPlatformClient.submit("android.user_contribution_screen", UserContributionEvent("ip_block"))
        }
    }
}
