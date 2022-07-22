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

package bisq.desktop.primary.main.content.components;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChannelKind;
import bisq.chat.ChatService;
import bisq.chat.channel.Channel;
import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.discuss.priv.PrivateDiscussionChannel;
import bisq.chat.discuss.priv.PrivateDiscussionChannelService;
import bisq.chat.discuss.priv.PrivateDiscussionChatMessage;
import bisq.chat.discuss.pub.PublicDiscussionChannel;
import bisq.chat.discuss.pub.PublicDiscussionChannelService;
import bisq.chat.discuss.pub.PublicDiscussionChatMessage;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.events.priv.PrivateEventsChannel;
import bisq.chat.events.priv.PrivateEventsChannelService;
import bisq.chat.events.priv.PrivateEventsChatMessage;
import bisq.chat.events.pub.PublicEventsChannel;
import bisq.chat.events.pub.PublicEventsChannelService;
import bisq.chat.events.pub.PublicEventsChatMessage;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Quotation;
import bisq.chat.support.SupportChannelSelectionService;
import bisq.chat.support.priv.PrivateSupportChannel;
import bisq.chat.support.priv.PrivateSupportChannelService;
import bisq.chat.support.priv.PrivateSupportChatMessage;
import bisq.chat.support.pub.PublicSupportChannel;
import bisq.chat.support.pub.PublicSupportChannelService;
import bisq.chat.support.pub.PublicSupportChatMessage;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.chat.trade.priv.PrivateTradeChatMessage;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.chat.trade.pub.PublicTradeChatMessage;
import bisq.chat.trade.pub.TradeChatOffer;
import bisq.common.monetary.Coin;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.utils.NoSelectionModel;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopupMenu;
import bisq.desktop.components.controls.BisqPopupMenuItem;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilteredListItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static bisq.desktop.primary.main.content.components.ChatMessagesComponent.View.EDITED_POST_FIX;

@Slf4j
public class ChatMessagesListView {
    private final Controller controller;

    public ChatMessagesListView(DefaultApplicationService applicationService,
                                Consumer<UserProfile> mentionUserHandler,
                                Consumer<ChatMessage> showChatUserDetailsHandler,
                                Consumer<ChatMessage> replyHandler,
                                ChannelKind channelKind,
                                boolean isCreateOfferTakerListMode,
                                boolean isCreateOfferPublishedMode) {
        controller = new Controller(applicationService,
                mentionUserHandler,
                showChatUserDetailsHandler,
                replyHandler,
                channelKind,
                isCreateOfferTakerListMode,
                isCreateOfferPublishedMode);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ObservableList<ChatMessageListItem<? extends ChatMessage>> getChatMessages() {
        return controller.model.getChatMessages();
    }

    public FilteredList<ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.model.getFilteredChatMessages();
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.setSearchPredicate(predicate);
    }

    public SortedList<ChatMessageListItem<? extends ChatMessage>> getSortedChatMessages() {
        return controller.model.getSortedChatMessages();
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }

    public void setCreateOfferCompleteHandler(Runnable createOfferCompleteHandler) {
        controller.setCreateOfferCompleteHandler(createOfferCompleteHandler);
    }

    public void setTakeOfferCompleteHandler(Runnable takeOfferCompleteHandler) {
        controller.setTakeOfferCompleteHandler(takeOfferCompleteHandler);
    }

    public void setComparator(Comparator<ReputationScore> comparing) {

    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final PrivateTradeChannelService privateTradeChannelService;
        private final PrivateDiscussionChannelService privateDiscussionChannelService;
        private final PublicDiscussionChannelService publicDiscussionChannelService;
        private final PublicTradeChannelService publicTradeChannelService;
        private final UserIdentityService userIdentityService;
        private final Consumer<UserProfile> mentionUserHandler;
        private final Consumer<ChatMessage> replyHandler;
        private final Consumer<ChatMessage> showChatUserDetailsHandler;
        private final ReputationService reputationService;
        private final UserProfileService userProfileService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final DiscussionChannelSelectionService discussionChannelSelectionService;
        private final SettingsService settingsService;
        private final PrivateEventsChannelService privateEventsChannelService;
        private final PublicEventsChannelService publicEventsChannelService;
        private final EventsChannelSelectionService eventsChannelSelectionService;
        private final PrivateSupportChannelService privateSupportChannelService;
        private final PublicSupportChannelService publicSupportChannelService;
        private final SupportChannelSelectionService supportChannelSelectionService;
        private Pin selectedChannelPin, chatMessagesPin;
        private Pin offerOnlySettingsPin;

        private Controller(DefaultApplicationService applicationService,
                           Consumer<UserProfile> mentionUserHandler,
                           Consumer<ChatMessage> showChatUserDetailsHandler,
                           Consumer<ChatMessage> replyHandler,
                           ChannelKind channelKind,
                           boolean isCreateOfferTakerListMode,
                           boolean isCreateOfferPublishedMode) {
            chatService = applicationService.getChatService();

            privateTradeChannelService = chatService.getPrivateTradeChannelService();
            publicTradeChannelService = chatService.getPublicTradeChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();

            privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
            publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();

            privateEventsChannelService = chatService.getPrivateEventsChannelService();
            publicEventsChannelService = chatService.getPublicEventsChannelService();
            eventsChannelSelectionService = chatService.getEventsChannelSelectionService();

            privateSupportChannelService = chatService.getPrivateSupportChannelService();
            publicSupportChannelService = chatService.getPublicSupportChannelService();
            supportChannelSelectionService = chatService.getSupportChannelSelectionService();

            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
            reputationService = applicationService.getUserService().getReputationService();
            settingsService = applicationService.getSettingsService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(chatService,
                    userIdentityService,
                    channelKind,
                    isCreateOfferTakerListMode,
                    isCreateOfferPublishedMode);
            view = new View(model, this);
        }

        public void setCreateOfferCompleteHandler(Runnable createOfferCompleteHandler) {
            model.createOfferCompleteHandler = Optional.of(createOfferCompleteHandler);
        }

        public void setTakeOfferCompleteHandler(Runnable takeOfferCompleteHandler) {
            model.takeOfferCompleteHandler = Optional.of(takeOfferCompleteHandler);
        }

        @Override
        public void onActivate() {
            offerOnlySettingsPin = FxBindings.subscribe(settingsService.getOffersOnly(), offerOnly -> applyPredicate());

            model.getSortedChatMessages().setComparator(ChatMessagesListView.ChatMessageListItem::compareTo);

            if (model.getChannelKind() == ChannelKind.TRADE) {
                selectedChannelPin = tradeChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (chatMessagesPin != null) {
                        chatMessagesPin.unbind();
                    }
                    if (channel instanceof PublicTradeChannel) {
                        chatMessagesPin = FxBindings.<PublicTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicTradeChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateTradeChannel) {
                        chatMessagesPin = FxBindings.<PrivateTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateTradeChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    } else if (channel == null) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        model.chatMessages.clear();
                    }
                });
            } else if (model.getChannelKind() == ChannelKind.DISCUSSION) {
                selectedChannelPin = discussionChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicDiscussionChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PublicDiscussionChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicDiscussionChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateDiscussionChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PrivateDiscussionChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateDiscussionChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            } else if (model.getChannelKind() == ChannelKind.EVENTS) {
                selectedChannelPin = eventsChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicEventsChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PublicEventsChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicEventsChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateEventsChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PrivateEventsChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateEventsChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            } else if (model.getChannelKind() == ChannelKind.SUPPORT) {
                selectedChannelPin = supportChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicSupportChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PublicSupportChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicSupportChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateSupportChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PrivateSupportChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateSupportChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            }
        }

        @Override
        public void onDeactivate() {
            offerOnlySettingsPin.unbind();
            selectedChannelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
                chatMessagesPin = null;
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // API - called from client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        public void setOfferOnly(boolean offerOnly) {
            model.offerOnly.set(offerOnly);
        }

        void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
            model.setSearchPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
            applyPredicate();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - delegate to client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onMention(UserProfile userProfile) {
            mentionUserHandler.accept(userProfile);
        }

        private void onShowChatUserDetails(ChatMessage chatMessage) {
            showChatUserDetailsHandler.accept(chatMessage);
        }

        private void onReply(ChatMessage chatMessage) {
            replyHandler.accept(chatMessage);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - handle internally
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onTakeOffer(PublicTradeChatMessage chatMessage) {
            if (model.isMyMessage(chatMessage) || chatMessage.getTradeChatOffer().isEmpty()) {
                return;
            }
            userProfileService.findUserProfile(chatMessage.getAuthorId())
                    .ifPresent(userProfile -> {
                        Optional<PrivateTradeChannel> channel = privateTradeChannelService.createAndAddChannel(userProfile);
                        channel.ifPresent(privateTradeChannel -> {
                            tradeChannelSelectionService.selectChannel(privateTradeChannel);
                            TradeChatOffer tradeChatOffer = chatMessage.getTradeChatOffer().get();
                            String text = chatMessage.getText();
                            Optional<Quotation> quotation = Optional.of(new Quotation(userProfile.getNym(),
                                    userProfile.getNickName(),
                                    userProfile.getPubKeyHash(),
                                    text));
                            String direction = Res.get(tradeChatOffer.getDirection().name().toLowerCase()).toUpperCase();
                            String amount = AmountFormatter.formatAmountWithCode(Coin.of(tradeChatOffer.getQuoteSideAmount(),
                                    tradeChatOffer.getMarket().getQuoteCurrencyCode()), true);
                            String methods = Joiner.on(", ").join(tradeChatOffer.getPaymentMethods());
                            String replyText = Res.get("satoshisquareapp.chat.takeOffer.takerRequest",
                                    direction, amount, methods);
                            privateTradeChannelService.sendPrivateChatMessage(replyText,
                                            quotation,
                                            privateTradeChannel)
                                    .thenAccept(result -> UIThread.run(() -> model.takeOfferCompleteHandler.ifPresent(Runnable::run)));
                        });
                    });
        }

        private void onDeleteMessage(ChatMessage chatMessage) {
            if (isMyMessage(chatMessage)) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
                if (chatMessage instanceof PublicTradeChatMessage) {
                    publicTradeChannelService.deleteChatMessage((PublicTradeChatMessage) chatMessage, userIdentity);
                } else if (chatMessage instanceof PublicDiscussionChatMessage) {
                    publicDiscussionChannelService.deleteChatMessage((PublicDiscussionChatMessage) chatMessage, userIdentity);
                } else if (chatMessage instanceof PublicEventsChatMessage) {
                    publicEventsChannelService.deleteChatMessage((PublicEventsChatMessage) chatMessage, userIdentity);
                } else if (chatMessage instanceof PublicSupportChatMessage) {
                    publicSupportChannelService.deleteChatMessage((PublicSupportChatMessage) chatMessage, userIdentity);
                }
                //todo delete private message
            }
        }

        private void onCreateOffer(PublicTradeChatMessage chatMessage) {
            UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
            publicTradeChannelService.publishChatMessage(chatMessage, userIdentity)
                    .thenAccept(result -> UIThread.run(() -> model.createOfferCompleteHandler.ifPresent(Runnable::run)));
        }

        private void onOpenPrivateChannel(ChatMessage chatMessage) {
            if (isMyMessage(chatMessage)) {
                return;
            }
            userProfileService.findUserProfile(chatMessage.getAuthorId()).ifPresent(this::createAndSelectPrivateChannel);
        }

        private void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            if (!isMyMessage(chatMessage)) {
                return;
            }
            if (chatMessage instanceof PublicTradeChatMessage) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
                publicTradeChannelService.publishEditedChatMessage((PublicTradeChatMessage) chatMessage, editedText, userIdentity);
            } else if (chatMessage instanceof PublicDiscussionChatMessage) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
                publicDiscussionChannelService.publishEditedChatMessage((PublicDiscussionChatMessage) chatMessage, editedText, userIdentity);
            }
            //todo editing private message not supported yet
        }

        private void onOpenMoreOptions(HBox reactionsBox, ChatMessage chatMessage, Runnable onClose) {
            if (chatMessage.equals(model.selectedChatMessageForMoreOptionsPopup.get())) {
                return;
            }
            model.selectedChatMessageForMoreOptionsPopup.set(chatMessage);

            List<BisqPopupMenuItem> items = new ArrayList<>();
            items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.copyMessage"),
                    () -> onCopyMessage(chatMessage)));
            if (!isMyMessage(chatMessage)) {
                items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.ignoreUser"),
                        () -> onIgnoreUser(chatMessage)));
                items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.reportUser"),
                        () -> onReportUser(chatMessage)));
            }

            BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
            menu.show(reactionsBox);
        }

        private void onReportUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorId()).ifPresent(author ->
                    chatService.reportUserProfile(author, ""));
        }

        private void onIgnoreUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorId())
                    .ifPresent(userProfileService::ignoreUserProfile);
        }

        private boolean isMyMessage(ChatMessage chatMessage) {
            return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        }

        private void createAndSelectPrivateChannel(UserProfile peer) {
            if (model.getChannelKind() == ChannelKind.TRADE) {
                privateTradeChannelService.createAndAddChannel(peer).ifPresent(tradeChannelSelectionService::selectChannel);
            } else if (model.getChannelKind() == ChannelKind.DISCUSSION) {
                privateDiscussionChannelService.createAndAddChannel(peer).ifPresent(discussionChannelSelectionService::selectChannel);
            } else if (model.getChannelKind() == ChannelKind.EVENTS) {
                privateEventsChannelService.createAndAddChannel(peer).ifPresent(eventsChannelSelectionService::selectChannel);
            } else if (model.getChannelKind() == ChannelKind.SUPPORT) {
                privateSupportChannelService.createAndAddChannel(peer).ifPresent(supportChannelSelectionService::selectChannel);
            }
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage>> predicate = item -> {
                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof PublicTradeChatMessage) {
                    PublicTradeChatMessage publicTradeChatMessage = (PublicTradeChatMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || publicTradeChatMessage.isOfferMessage();
                }
                return offerOnlyPredicate &&
                        item.getSenderUserProfile().isPresent() &&
                        !userProfileService.getIgnoredUserProfileIds().contains(item.getSenderUserProfile().get().getId()) &&
                        userProfileService.findUserProfile(item.getSenderUserProfile().get().getId()).isPresent();
            };
            model.filteredChatMessages.setPredicate(item -> model.getSearchPredicate().test(item) && predicate.test(item));
        }

        private void onCopyMessage(ChatMessage chatMessage) {
            ClipboardUtil.copyToClipboard(chatMessage.getText());
        }
    }


    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final UserIdentityService userIdentityService;
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final BooleanProperty allowEditing = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> selectedChatMessageForMoreOptionsPopup = new SimpleObjectProperty<>(null);
        private final ChannelKind channelKind;
        private final boolean isCreateOfferTakerListMode;
        private final boolean isCreateOfferPublishedMode;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage>> searchPredicate = e -> true;
        private Optional<Runnable> createOfferCompleteHandler = Optional.empty();
        private Optional<Runnable> takeOfferCompleteHandler = Optional.empty();
        private final BooleanProperty offerOnly = new SimpleBooleanProperty();

        private Model(ChatService chatService,
                      UserIdentityService userIdentityService,
                      ChannelKind channelKind,
                      boolean isCreateOfferTakerListMode,
                      boolean isCreateOfferPublishedMode) {
            this.chatService = chatService;
            this.userIdentityService = userIdentityService;
            this.channelKind = channelKind;
            this.isCreateOfferTakerListMode = isCreateOfferTakerListMode;
            this.isCreateOfferPublishedMode = isCreateOfferPublishedMode;
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        }

        boolean isOfferMessage(ChatMessage chatMessage) {
            return chatMessage instanceof PublicTradeChatMessage &&
                    ((PublicTradeChatMessage) chatMessage).isOfferMessage();
        }
    }


    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final ListView<ChatMessageListItem<? extends ChatMessage>> messagesListView;

        private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            messagesListView = new ListView<>(model.getSortedChatMessages());
            messagesListView.getStyleClass().add("chat-messages-list-view");

            Label placeholder = new Label(Res.get("noData"));
            messagesListView.setPlaceholder(placeholder);
            messagesListView.setCellFactory(getCellFactory());

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            messagesListView.setSelectionModel(new NoSelectionModel<>());

            VBox.setVgrow(messagesListView, Priority.ALWAYS);
            VBox.setMargin(messagesListView, new Insets(0, 0, 0, 24));
            root.getChildren().addAll(messagesListView);

            messagesListener = c -> UIThread.runOnNextRenderFrame(this::scrollDown);
        }

        @Override
        protected void onViewAttached() {
            model.getSortedChatMessages().addListener(messagesListener);
            UIThread.runOnNextRenderFrame(this::scrollDown);
        }

        @Override
        protected void onViewDetached() {
            model.getSortedChatMessages().removeListener(messagesListener);
        }

        private void scrollDown() {
            if (!model.isCreateOfferTakerListMode) {
                messagesListView.scrollTo(messagesListView.getItems().size() - 1);
            }
        }

        public Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                    return new ListCell<>() {
                        private final Label userName, dateTime,
                                replyButton, pmButton, editButton, deleteButton, moreOptionsButton;
                        private final Text message, quotedMessageField;
                        private final BisqTextArea editInputField;
                        private final Button actionButton, saveEditButton, cancelEditButton;
                        private final VBox reputationVBox;
                        private final HBox hBox, messageHBox, reactionsHBox, editButtonsHBox, quotedMessageHBox;
                        private final ChatUserIcon chatUserIcon = new ChatUserIcon(42);
                        private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
                        private Subscription widthSubscription;

                        {
                            // HBox for user name + date
                            userName = new Label();
                            userName.setId("chat-user-name");

                            dateTime = new Label();
                            dateTime.setId("chat-messages-date");
                            HBox userInfoHBox = new HBox(5, userName, dateTime);

                            // quoted message
                            quotedMessageField = new Text();
                            quotedMessageHBox = new HBox(10);
                            quotedMessageHBox.setVisible(false);
                            quotedMessageHBox.setManaged(false);

                            // HBox for message reputation vBox and action button
                            message = new Text();
                            message.setId("chat-messages-message");

                            // VBox for  reputation label and score
                            Label reputationLabel = new Label(Res.get("reputation").toUpperCase());
                            reputationLabel.getStyleClass().add("bisq-text-7");
                            reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
                            reputationVBox.setAlignment(Pos.CENTER_LEFT);

                            // Take offer or delete offer button
                            actionButton = new Button();
                            actionButton.setVisible(false);
                            actionButton.setManaged(false);

                            // edit
                            editInputField = new BisqTextArea();
                            editInputField.setId("chat-messages-edit-text-area");
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);

                            // edit buttons
                            saveEditButton = new Button(Res.get("save"));
                            saveEditButton.setDefaultButton(true);
                            cancelEditButton = new Button(Res.get("cancel"));

                            editButtonsHBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);

                            // HBox of message, editInputField, reputation VBox and button
                            HBox.setMargin(actionButton, new Insets(0, 10, 0, 0));
                            HBox.setHgrow(message, Priority.ALWAYS);
                            HBox.setMargin(editInputField, new Insets(-5, 0, -20, -10));
                            messageHBox = new HBox(15, message, editInputField, Spacer.fillHBox(), reputationVBox, actionButton);
                            messageHBox.setFillHeight(true);
                            messageHBox.setPadding(new Insets(15));
                            messageHBox.setAlignment(Pos.CENTER_LEFT);

                            // Reactions box
                            replyButton = Icons.getIcon(AwesomeIcon.REPLY);
                            replyButton.setCursor(Cursor.HAND);
                            pmButton = Icons.getIcon(AwesomeIcon.COMMENT_ALT);
                            pmButton.setCursor(Cursor.HAND);
                            editButton = Icons.getIcon(AwesomeIcon.EDIT);
                            editButton.setCursor(Cursor.HAND);
                            deleteButton = Icons.getIcon(AwesomeIcon.REMOVE_SIGN);
                            deleteButton.setCursor(Cursor.HAND);
                            moreOptionsButton = Icons.getIcon(AwesomeIcon.ELLIPSIS_HORIZONTAL);
                            moreOptionsButton.setCursor(Cursor.HAND);
                            reactionsHBox = new HBox(20, replyButton, pmButton, editButton, deleteButton, moreOptionsButton);
                            reactionsHBox.setPadding(new Insets(0, 15, 5, 15));
                            reactionsHBox.setVisible(false);
                            reactionsHBox.setAlignment(Pos.CENTER_RIGHT);

                            VBox.setMargin(quotedMessageHBox, new Insets(15, 0, 10, 5));
                            VBox vBox = new VBox(0, userInfoHBox, quotedMessageHBox, messageHBox, editButtonsHBox, reactionsHBox);

                            HBox.setHgrow(vBox, Priority.ALWAYS);
                            hBox = new HBox(15, chatUserIcon, vBox);

                            hBox.setPadding(new Insets(0, 24, 0, 0));
                        }

                        private void hideReactionsBox() {
                            reactionsHBox.setVisible(false);
                        }

                        @Override
                        public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {

                                ChatMessage chatMessage = item.getChatMessage();
                                handleQuoteMessageBox(item);
                                handleReactionsBox(item);
                                handleEditBox(chatMessage);

                                message.setText(item.getMessage());
                                dateTime.setText(item.getDate());

                                item.getSenderUserProfile().ifPresent(author -> {
                                    userName.setText(author.getUserName());
                                    userName.setOnMouseClicked(e -> controller.onMention(author));

                                    chatUserIcon.setChatUser(author);
                                    chatUserIcon.setCursor(Cursor.HAND);
                                    Tooltip.install(chatUserIcon, new Tooltip(author.getTooltipString()));
                                    chatUserIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(chatMessage));

                                    reputationScoreDisplay.applyReputationScore(item.getReputationScore());
                                });

                                boolean isOfferMessage = model.isOfferMessage(chatMessage);
                                boolean isCreateOfferMakerListMode = false;

                                actionButton.setVisible(isOfferMessage && !model.isCreateOfferPublishedMode);
                                actionButton.setManaged(isOfferMessage && !model.isCreateOfferPublishedMode);

                                messageHBox.getStyleClass().remove("chat-offer-box");
                                messageHBox.getStyleClass().remove("chat-offer-box-my-offer");
                                actionButton.getStyleClass().remove("red-button");
                                actionButton.getStyleClass().remove("default-button");

                                if (isOfferMessage) {
                                    messageHBox.getStyleClass().add("chat-offer-box");
                                    boolean myMessage = model.isMyMessage(chatMessage);
                                    if (myMessage) {
                                        messageHBox.getStyleClass().remove("chat-offer-box");
                                        messageHBox.getStyleClass().add("chat-offer-box-my-offer");

                                        if (isCreateOfferMakerListMode) {
                                            // used by create offer view / maker list
                                            actionButton.setText(Res.get("createOffer"));
                                            actionButton.getStyleClass().add("default-button");
                                            actionButton.setOnAction(e -> controller.onCreateOffer((PublicTradeChatMessage) chatMessage));
                                        } else {
                                            actionButton.setText(Res.get("deleteOffer"));
                                            actionButton.getStyleClass().add("red-button");
                                            actionButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                                        }
                                    } else {
                                        actionButton.setText(Res.get("takeOffer"));
                                        actionButton.getStyleClass().add("default-button");
                                        actionButton.setOnAction(e -> controller.onTakeOffer((PublicTradeChatMessage) chatMessage));
                                    }

                                    VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));
                                    HBox.setMargin(reputationVBox, new Insets(-5, 10, 0, 0));
                                    VBox.setMargin(reactionsHBox, new Insets(3, 0, -10, 0));
                                    VBox.setMargin(editButtonsHBox, new Insets(12, 0, -14, 0));
                                } else {
                                    messageHBox.getStyleClass().remove("chat-offer-box-my-offer");
                                    messageHBox.getStyleClass().remove("chat-offer-box");
                                    VBox.setMargin(messageHBox, new Insets(-5, 0, 0, 0));
                                    HBox.setMargin(reputationVBox, new Insets(-36, 10, 0, 0));
                                    VBox.setMargin(reactionsHBox, new Insets(-8, 0, -10, 0));
                                    VBox.setMargin(editButtonsHBox, new Insets(-10, 0, -3, 0));
                                }

                                editInputField.maxWidthProperty().bind(message.wrappingWidthProperty());
                                widthSubscription = EasyBind.subscribe(messagesListView.widthProperty(),
                                        w -> adjustMessageWidth(item));
                                UIThread.runOnNextRenderFrame(() -> adjustMessageWidth(item));
                                setGraphic(hBox);
                            } else {
                                if (widthSubscription != null) {
                                    widthSubscription.unsubscribe();
                                }

                                // actionButton.getStyleClass().remove("red-button");

                                editInputField.maxWidthProperty().unbind();

                                saveEditButton.setOnAction(null);
                                cancelEditButton.setOnAction(null);
                                actionButton.setOnAction(null);

                                userName.setOnMouseClicked(null);
                                chatUserIcon.setOnMouseClicked(null);
                                replyButton.setOnMouseClicked(null);
                                pmButton.setOnMouseClicked(null);
                                editButton.setOnMouseClicked(null);
                                deleteButton.setOnMouseClicked(null);
                                moreOptionsButton.setOnMouseClicked(null);

                                editInputField.setOnKeyPressed(null);

                                hBox.setOnMouseEntered(null);
                                hBox.setOnMouseExited(null);

                                chatUserIcon.releaseResources();

                                setGraphic(null);
                            }
                        }

                        private void adjustMessageWidth(ChatMessageListItem<? extends ChatMessage> item) {
                            if (item == null) {
                                return;
                            }
                            double width = messagesListView.getWidth() - 95;
                            double actionButtonPadding = actionButton.getWidth() > 0 ? 40 : 0;
                            double actionButtonWidth = actionButton.getWidth() + actionButtonPadding;
                            double reputationVBoxWidth = reputationVBox.getWidth();
                            if (actionButton.isVisible() && actionButtonWidth == 0) {
                                return;
                            }
                            if (reputationVBox.isVisible() && reputationVBoxWidth == 0) {
                                return;
                            }
                            double wrappingWidth = width - actionButtonWidth - reputationVBoxWidth - 60;

                            message.setWrappingWidth(wrappingWidth);
                            quotedMessageField.setWrappingWidth(wrappingWidth);
                        }

                        private void handleEditBox(ChatMessage chatMessage) {
                            saveEditButton.setOnAction(e -> {
                                controller.onSaveEditedMessage(chatMessage, editInputField.getText());
                                onCloseEditMessage();
                            });
                            cancelEditButton.setOnAction(e -> onCloseEditMessage());
                        }

                        private void handleReactionsBox(ChatMessageListItem<? extends ChatMessage> item) {
                            ChatMessage chatMessage = item.getChatMessage();
                            replyButton.setOnMouseClicked(e -> controller.onReply(chatMessage));
                            pmButton.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
                            editButton.setOnMouseClicked(e -> onEditMessage(item));
                            deleteButton.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
                            moreOptionsButton.setOnMouseClicked(e -> controller.onOpenMoreOptions(reactionsHBox, chatMessage, () -> {
                                hideReactionsBox();
                                model.selectedChatMessageForMoreOptionsPopup.set(null);
                            }));

                            boolean isMyMessage = model.isMyMessage(chatMessage);
                            replyButton.setVisible(!isMyMessage);
                            replyButton.setManaged(!isMyMessage);
                            pmButton.setVisible(!isMyMessage);
                            pmButton.setManaged(!isMyMessage);
                            boolean allowEditing = model.allowEditing.get();
                            editButton.setVisible(isMyMessage && allowEditing);
                            editButton.setManaged(isMyMessage && allowEditing);
                            deleteButton.setVisible(isMyMessage && allowEditing);
                            deleteButton.setManaged(isMyMessage && allowEditing);

                            setOnMouseEntered(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() != null || editInputField.isVisible()) {
                                    return;
                                }
                                reactionsHBox.setVisible(true);
                            });
                            setOnMouseExited(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() == null) {
                                    hideReactionsBox();
                                }
                            });
                        }

                        private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage> item) {
                            Optional<Quotation> optionalQuotation = item.getQuotedMessage();
                            if (optionalQuotation.isPresent()) {
                                Quotation quotation = optionalQuotation.get();
                                if (quotation.isValid()) {
                                    quotedMessageHBox.setVisible(true);
                                    quotedMessageHBox.setManaged(true);

                                    Region verticalLine = new Region();
                                    verticalLine.setStyle("-fx-background-color: -bisq-grey-dimmed");
                                    verticalLine.setMinWidth(3);
                                    verticalLine.setMinHeight(25);

                                    quotedMessageField.setText(quotation.getMessage());
                                    quotedMessageField.setStyle("-fx-fill: -bisq-grey-dimmed");

                                    ImageView roboIconImageView = new ImageView();
                                    roboIconImageView.setFitWidth(25);
                                    roboIconImageView.setFitHeight(25);
                                    Image image = RoboHash.getImage(quotation.getPubKeyHash());
                                    roboIconImageView.setImage(image);

                                    Label userName = new Label(quotation.getUserName());
                                    userName.setPadding(new Insets(4, 0, 0, 0));
                                    userName.setStyle("-fx-text-fill: -bisq-grey-dimmed");

                                    HBox.setMargin(roboIconImageView, new Insets(0, 0, 0, -5));
                                    HBox iconAndUserName = new HBox(15, roboIconImageView, userName);
                                    iconAndUserName.setSpacing(5);

                                    VBox contentVBox = new VBox(5, iconAndUserName, quotedMessageField);

                                    HBox.setMargin(verticalLine, new Insets(0, 0, 0, 5));
                                    quotedMessageHBox.getChildren().setAll(verticalLine, contentVBox);
                                    UIThread.runOnNextRenderFrame(() -> verticalLine.setMinHeight(contentVBox.getHeight() - 10));
                                }
                            } else {
                                quotedMessageHBox.getChildren().clear();
                                quotedMessageHBox.setVisible(false);
                                quotedMessageHBox.setManaged(false);
                            }
                        }

                        private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                            reactionsHBox.setVisible(false);
                            editInputField.setVisible(true);
                            editInputField.setManaged(true);
                            editInputField.setInitialHeight(message.getBoundsInLocal().getHeight());
                            editInputField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                            // editInputField.setScrollHideThreshold(200);
                            editInputField.requestFocus();
                            editInputField.positionCaret(message.getText().length());
                            editButtonsHBox.setVisible(true);
                            editButtonsHBox.setManaged(true);
                            message.setVisible(false);
                            message.setManaged(false);

                            editInputField.setOnKeyPressed(event -> {
                                if (event.getCode() == KeyCode.ENTER) {
                                    event.consume();
                                    if (event.isShiftDown()) {
                                        editInputField.appendText(System.getProperty("line.separator"));
                                    } else if (!editInputField.getText().isEmpty()) {
                                        controller.onSaveEditedMessage(item.getChatMessage(), editInputField.getText().trim());
                                        onCloseEditMessage();
                                    }
                                }
                            });
                        }

                        private void onCloseEditMessage() {
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);
                            message.setVisible(true);
                            message.setManaged(true);
                            editInputField.setOnKeyPressed(null);
                        }
                    };
                }
            };
        }
    }

    @Slf4j
    @Getter
    @EqualsAndHashCode
    public static class ChatMessageListItem<T extends ChatMessage> implements Comparable<ChatMessageListItem<T>>, FilteredListItem {
        private final T chatMessage;
        private final String message;
        private final String date;
        private final Optional<Quotation> quotedMessage;
        private final Optional<UserProfile> senderUserProfile;
        private final String nym;
        private final String nickName;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;

        public ChatMessageListItem(T chatMessage, UserProfileService userProfileService, ReputationService reputationService) {
            this.chatMessage = chatMessage;

            if (chatMessage instanceof PrivateTradeChatMessage) {
                senderUserProfile = Optional.of(((PrivateTradeChatMessage) chatMessage).getSender());
            } else if (chatMessage instanceof PrivateDiscussionChatMessage) {
                senderUserProfile = Optional.of(((PrivateDiscussionChatMessage) chatMessage).getSender());
            } else {
                senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorId());
            }
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            quotedMessage = chatMessage.getQuotation();
            date = DateFormatter.formatDateTimeV2(new Date(chatMessage.getDate()));

            nym = senderUserProfile.map(UserProfile::getNym).orElse("");
            nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");

            reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);
        }

        @Override
        public int compareTo(ChatMessageListItem o) {
            return new Date(chatMessage.getDate()).compareTo(new Date(o.getChatMessage().getDate()));
        }

        @Override
        public boolean match(String filterString) {
            return filterString == null || filterString.isEmpty() || StringUtils.containsIgnoreCase(message, filterString) || StringUtils.containsIgnoreCase(nym, filterString) || StringUtils.containsIgnoreCase(nickName, filterString) || StringUtils.containsIgnoreCase(date, filterString);
        }
    }
}