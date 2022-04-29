/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.social.chat.channels;

import bisq.common.observable.ObservableSet;
import bisq.social.chat.NotificationSetting;
import bisq.social.chat.messages.PrivateTradeChatMessage;
import bisq.social.user.ChatUserProfile;
import bisq.social.user.profile.ChatUserIdentity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PrivateTradeChannel extends Channel<PrivateTradeChatMessage> implements PrivateChannel {
    private final ChatUserProfile peer;
    private final ChatUserIdentity myProfile;
    private final ObservableSet<PrivateTradeChatMessage> chatMessages = new ObservableSet<>();

    public PrivateTradeChannel(ChatUserProfile peer, ChatUserIdentity myProfile) {
        this(PrivateChannel.createChannelId(peer.getNym(), myProfile.getProfileId()),
                peer,
                myProfile,
                NotificationSetting.ALL,
                new HashSet<>());
    }

    public PrivateTradeChannel(String id, ChatUserProfile peer, ChatUserIdentity myProfile) {
        this(id, peer, myProfile, NotificationSetting.ALL, new HashSet<>());
    }

    private PrivateTradeChannel(String id,
                                ChatUserProfile peer,
                                ChatUserIdentity myProfile,
                                NotificationSetting notificationSetting,
                                Set<PrivateTradeChatMessage> chatMessages) {
        super(id, notificationSetting);
        this.peer = peer;
        this.myProfile = myProfile;
        this.chatMessages.addAll(chatMessages);
    }

    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateTradeChannel(bisq.social.protobuf.PrivateTradeChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyChatUserIdentity(myProfile.toProto())
                        .addAllChatMessages(chatMessages.stream().map(this::getChatMessageProto).collect(Collectors.toList())))
                .build();
    }

    public static PrivateTradeChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                                bisq.social.protobuf.PrivateTradeChannel proto) {
        return new PrivateTradeChannel(
                baseProto.getId(),
                ChatUserProfile.fromProto(proto.getPeer()),
                ChatUserIdentity.fromProto(proto.getMyChatUserIdentity()),
                NotificationSetting.fromProto(baseProto.getNotificationSetting()),
                proto.getChatMessagesList().stream()
                        .map(PrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(PrivateTradeChatMessage chatMessage) {
        return chatMessage.toChatMessageProto();
    }

    @Override
    public void addChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PrivateTradeChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return peer.getUserName();
    }
}