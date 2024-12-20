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

package bisq.desktop.overlay.update;

import bisq.common.observable.Pin;
import bisq.common.platform.PlatformUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.evolution.updater.DownloadItem;
import bisq.evolution.updater.UpdaterService;
import bisq.evolution.updater.UpdaterUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CancellationException;

import static bisq.evolution.updater.UpdaterUtils.RELEASES_URL;

@Slf4j
public class UpdaterController implements Controller {
    private final UpdaterModel model;
    @Getter
    private final UpdaterView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;
    private final UpdaterService updaterService;
    private Pin getDownloadInfoListPin, releaseNotificationPin;

    public UpdaterController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        updaterService = serviceProvider.getUpdaterService();
        model = new UpdaterModel();
        view = new UpdaterView(model, this);
    }

    @Override
    public void onActivate() {
        getDownloadInfoListPin = FxBindings.<DownloadItem, UpdaterView.ListItem>bind(model.getListItems())
                .map(UpdaterView.ListItem::new)
                .to(updaterService.getDownloadItemList());

        releaseNotificationPin = updaterService.getReleaseNotification().addObserver(releaseNotification -> {
            if (releaseNotification == null) {
                return;
            }
            UIThread.run(() -> {
                String version = releaseNotification.getVersionString();
                model.getVersion().set(version);
                model.getReleaseNotes().set(releaseNotification.getReleaseNotes());
                model.getDownloadUrl().set(RELEASES_URL + version);

                boolean isLauncherUpdate = releaseNotification.isLauncherUpdate();
                model.getIsLauncherUpdate().set(isLauncherUpdate);

                model.getHeadline().set(isLauncherUpdate ?
                        Res.get("updater.headline.isLauncherUpdate") :
                        Res.get("updater.headline"));
                model.getFurtherInfo().set(isLauncherUpdate ?
                        Res.get("updater.furtherInfo.isLauncherUpdate") :
                        Res.get("updater.furtherInfo"));
                model.getVerificationInfo().set(isLauncherUpdate ?
                        Res.get("updater.downloadAndVerify.info.isLauncherUpdate") :
                        Res.get("updater.downloadAndVerify.info"));
                model.getShutDownButtonText().set(isLauncherUpdate ?
                        Res.get("updater.shutDown.isLauncherUpdate") :
                        Res.get("updater.shutDown"));
            });
        });

        model.getFilteredList().setPredicate(e -> !e.getDownloadItem().getDestinationFile().getName().startsWith(UpdaterUtils.FROM_BISQ_WEBPAGE_PREFIX));
    }

    @Override
    public void onDeactivate() {
        getDownloadInfoListPin.unbind();
        releaseNotificationPin.unbind();
    }

    void onDownload() {
        model.getTableVisible().set(true);
        model.getHeadline().set(Res.get("updater.downloadAndVerify.headline"));
        try {
            updaterService.downloadAndVerify()
                    .whenComplete((nil, throwable) -> {
                        if (throwable == null) {
                            UIThread.run(() -> model.getDownloadAndVerifyCompleted().set(true));
                        } else if (!(throwable instanceof CancellationException)) {
                            UIThread.run(() -> new Popup().error(throwable).show());
                        }
                    });
        } catch (IOException e) {
            UIThread.run(() -> new Popup().error(e).show());
        }
    }

    void onDownloadLater() {
        OverlayController.hide();
    }

    void onIgnore() {
        settingsService.setCookie(CookieKey.IGNORE_VERSION, model.getVersion().get(), true);
        OverlayController.hide();
    }

    void onShutdown() {
        if (updaterService.getReleaseNotification().get().isLauncherUpdate()) {
            PlatformUtils.open(PlatformUtils.getDownloadOfHomeDir());
        }
         serviceProvider.getShutDownHandler().shutdown();
    }

    void onClose() {
        OverlayController.hide();
    }

    void onOpenUrl() {
        Browser.open(model.getDownloadUrl().get());
    }
}
