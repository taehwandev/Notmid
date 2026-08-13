package app.thdev.glassnavlab.core.model.notmid

data class NotmidChatAccess(
    val relationship: NotmidChatRelationship,
    val inviteStatus: NotmidChatInviteStatus,
    val canSendMessage: Boolean,
    val canAcceptInvite: Boolean,
    val canRejectInvite: Boolean,
    val reasonLabel: String,
) {
    companion object {
        val AcceptedFriend = NotmidChatAccess(
            relationship = NotmidChatRelationship.Friend,
            inviteStatus = NotmidChatInviteStatus.Accepted,
            canSendMessage = true,
            canAcceptInvite = false,
            canRejectInvite = false,
            reasonLabel = "Friends can chat immediately.",
        )
    }
}
