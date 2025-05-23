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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.desktop.navigation.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.overlay.OverlayController;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import lombok.Getter;

import java.util.stream.Collectors;

public class ProfileCardOffersController implements Controller {
    @Getter
    private final ProfileCardOffersView view;
    private final ProfileCardOffersModel model;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final ReputationService reputationService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookSelectionService bisqEasyOfferbookChannelSelectionService;
    private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;

    public ProfileCardOffersController(ServiceProvider serviceProvider) {
        model = new ProfileCardOffersModel();
        view = new ProfileCardOffersView(model, this);
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookChannelSelectionService = serviceProvider.getChatService().getBisqEasyOfferbookChannelSelectionService();
        reputationService = serviceProvider.getUserService().getReputationService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyOfferbookMessageService = serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
        model.getOfferbookListItems().forEach(ProfileCardOfferListItem::dispose);
    }

    public void setUserProfile(UserProfile userProfile) {
        model.getOfferbookListItems().setAll(bisqEasyOfferbookMessageService.getAllOfferbookMessagesWithOffer(userProfile.getId())
                .map(userChatMessageWithOffer -> new ProfileCardOfferListItem(
                        userChatMessageWithOffer, userProfile, reputationService, marketPriceService))
                .collect(Collectors.toList()));
    }

    public String getNumberOffers() {
        return String.valueOf(model.getOfferbookListItems().size());
    }

    void onGoToOfferbookMessage(BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        bisqEasyOfferbookChannelService.findChannel(bisqEasyOfferbookMessage.getChannelId())
                .ifPresent(channel -> {
                    bisqEasyOfferbookChannelSelectionService.selectChannel(channel);
                    channel.getHighlightedMessage().set(bisqEasyOfferbookMessage);
                });

        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget.BISQ_EASY_OFFERBOOK));
    }
}
