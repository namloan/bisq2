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

package bisq.desktop.main.content.mu_sig.components;

import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MuSigProtocolWaitingAnimation extends StackPane {
    public static final int INTERVAL = 1000;

    private final ImageView spinningCircle;
    private ImageView waitingStateIcon;
    private MuSigProtocolWaitingState muSigProtocolWaitingState;
    private final RotateTransition rotate;
    private final FadeTransition fadeTransition;
    private Scene scene;
    private ChangeListener<Scene> sceneListener;
    private ChangeListener<Boolean> focusListener;
    private UIScheduler uiScheduler;

    public MuSigProtocolWaitingAnimation(MuSigProtocolWaitingState muSigProtocolWaitingState) {
        setState(muSigProtocolWaitingState);

        setAlignment(Pos.CENTER);

        spinningCircle = ImageUtil.getImageViewById(getSpinningCircleIconId(muSigProtocolWaitingState));
        spinningCircle.setFitHeight(78);
        spinningCircle.setFitWidth(78);
        spinningCircle.setPreserveRatio(true);

        getChildren().add(spinningCircle);

        fadeTransition = new FadeTransition(ManagedDuration.millis(INTERVAL), spinningCircle);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        rotate = new RotateTransition(ManagedDuration.millis(INTERVAL), spinningCircle);
        rotate.setByAngle(360);

        sceneListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                focusListener = new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable,
                                        Boolean oldValue,
                                        Boolean newValue) {
                        if (newValue) {
                            rotate.playFromStart();
                            spinningCircle.setOpacity(0);
                            fadeTransition.playFromStart();
                        }
                    }
                };
                scene = getScene();
                scene.getWindow().focusedProperty().addListener(focusListener);
            } else {
                if (scene != null) {
                    scene.getWindow().focusedProperty().removeListener(focusListener);
                    scene = null;
                }
                sceneProperty().removeListener(sceneListener);
            }
        };
        sceneProperty().addListener(sceneListener);
    }

    public void setState(MuSigProtocolWaitingState state) {
        if (muSigProtocolWaitingState != state) {
            muSigProtocolWaitingState = state;
            updateWaitingStateIcon();
            if (spinningCircle != null) {
                spinningCircle.setImage(ImageUtil.getImageViewById(getSpinningCircleIconId(state)).getImage());
            }
        }
    }

    private void updateWaitingStateIcon() {
        if (waitingStateIcon != null) {
            getChildren().remove(waitingStateIcon);
            waitingStateIcon = null;
        }

        if (muSigProtocolWaitingState != null) {
            waitingStateIcon = ImageUtil.getImageViewById(getWaitingStateIconId(muSigProtocolWaitingState));
            getChildren().add(waitingStateIcon);
        }
    }

    private String getWaitingStateIconId(MuSigProtocolWaitingState muSigProtocolWaitingState) {
        return switch (muSigProtocolWaitingState) {
            case TAKE_OFFER -> "take-bisq-easy-offer";
            case ACCOUNT_DATA -> "account-data";
            case FIAT_PAYMENT -> "fiat-payment";
            case FIAT_PAYMENT_CONFIRMATION -> "fiat-payment-confirmation";
            case BITCOIN_ADDRESS -> "bitcoin-address";
            case BITCOIN_PAYMENT -> "bitcoin-payment";
            case BITCOIN_CONFIRMATION -> "bitcoin-confirmation";
            case SCAN_WITH_CAMERA -> "scan-with-camera";
            case TRADE_COMPLETED -> "take-bisq-easy-offer";
        };
    }

    private String getSpinningCircleIconId(MuSigProtocolWaitingState state) {
        return state == MuSigProtocolWaitingState.TAKE_OFFER ? "take-bisq-easy-offer-circle" : "spinning-circle";
    }

    public void play() {
        rotate.play();
        fadeTransition.play();
    }

    public void playIndefinitely() {
        playRepeated(0, 4 * INTERVAL, TimeUnit.MILLISECONDS, Long.MAX_VALUE);
    }

    public void playRepeated(long initialDelay, long delay, TimeUnit timeUnit, long cycles) {
        stop();
        uiScheduler = UIScheduler.run((this::play)).repeated(initialDelay, delay, timeUnit, cycles);
    }

    public void stop() {
        rotate.stop();
        fadeTransition.stop();
        if (uiScheduler != null) {
            uiScheduler.stop();
            uiScheduler = null;
        }
    }
}
